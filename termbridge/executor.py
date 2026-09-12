"""Выполнение команд ИИ в настоящем шелле.

* Linux / macOS — постоянная PTY-сессия bash: `cd`, `export`, `source venv/bin/activate`
  и т.п. сохраняются между командами, ровно как в обычном терминале.
* Windows — каждая команда выполняется в отдельном PowerShell, рабочая папка
  сохраняется вручную по маркеру пути (не требует сторонних пакетов).

После команды шелл печатает служебные строки-маркеры, по которым мост понимает,
что вывод закончен, узнаёт код возврата и текущую директорию:

    __TB_CWD__/path
    __TB_MARK__<id>:<код возврата>
"""

from __future__ import annotations

import base64
import errno
import fcntl
import os
import platform
import pty
import re
import select
import signal
import struct
import subprocess
import termios
import time
from dataclasses import dataclass

_ANSI_RE = re.compile(
    r"(?:\x1b\[[0-?]*[ -/]*[@-~]"          # CSI: цвета, перемещения курсора
    r"|\x1b\][^\x07\x1b]*(?:\x07|\x1b\\)"  # OSC: заголовки окон
    r"|\x1b[=>]|\x1b[()][0-9A-B])"         # прочие терминальные переключения
)

_MARK_RE = re.compile(r"__TB_MARK__(\d+):(-?\d+)")
_CWD_RE = re.compile(r"__TB_CWD__(.*)")


@dataclass
class ExecResult:
    command: str
    output: str
    returncode: int | None
    timed_out: bool
    duration: float
    cwd: str

    @property
    def ok(self) -> bool:
        return not self.timed_out and self.returncode == 0


def _normalize_output(raw: str, strip_ansi: bool) -> str:
    if strip_ansi:
        raw = _ANSI_RE.sub("", raw)
    raw = raw.replace("\r\n", "\n").replace("\r", "\n")
    # Убираем наши служебные маркерные строки, если они попали в вывод.
    lines = [
        ln for ln in raw.split("\n")
        if not ln.startswith(("__TB_MARK__", "__TB_CWD__", "__TB_READY__"))
    ]
    return "\n".join(lines).strip("\n")


class ShellSession:
    """Общий интерфейс шелла для моста."""

    def __init__(self, cwd: str, timeout: float = 600, strip_ansi: bool = True,
                 shell_path: str | None = None):
        self.cwd = os.path.abspath(os.path.expanduser(cwd))
        self.timeout = timeout
        self.strip_ansi = strip_ansi
        self.shell_path = shell_path
        self._counter = 0

    # --- фабрика -----------------------------------------------------------
    @staticmethod
    def create(os_name: str = "auto", **kwargs) -> "ShellSession":
        os_name = detect_os() if os_name == "auto" else os_name
        if os_name in ("linux", "macos"):
            return _PtyShell(**kwargs)
        if os_name == "windows":
            return _WinShell(**kwargs)
        raise ValueError(f"Неизвестная ОС: {os_name}")

    def run(self, code: str, timeout: float | None = None) -> ExecResult:
        raise NotImplementedError

    def close(self) -> None:
        pass


def detect_os() -> str:
    sysname = platform.system().lower()
    if sysname.startswith("win"):
        return "windows"
    if sysname == "darwin":
        return "macos"
    return "linux"


def os_human_name(os_name: str) -> str:
    return {
        "linux": "Linux (Bash)",
        "macos": "macOS (Bash/Zsh)",
        "windows": "Windows (PowerShell)",
    }[os_name]


class _PtyShell(ShellSession):
    """Постоянная интерактивная сессия bash в PTY (POSIX)."""

    def start(self) -> None:
        shell = self.shell_path or "/bin/bash"
        self.pid, self.fd = pty.fork()
        if self.pid == 0:
            # --- дочерний процесс: превращаемся в шелл ---
            try:
                os.chdir(self.cwd)
            except OSError:
                pass
            env = os.environ.copy()
            env["TERM"] = "dumb"
            env["PS1"] = ""
            env["PS2"] = ""
            env["LANG"] = env.get("LANG") or "C.UTF-8"
            os.execvpe(shell, [shell], env)
            os._exit(127)

        # --- родительский процесс ---
        # Широкое «окно», чтобы вывод не переносился на 80 колонках.
        fcntl.ioctl(self.fd, termios.TIOCSWINSZ, struct.pack("HHHH", 60, 240, 0, 0))
        fl = fcntl.fcntl(self.fd, fcntl.F_GETFL)
        fcntl.fcntl(self.fd, fcntl.F_SETFL, fl | os.O_NONBLOCK)

        # Без эха ввода (чтобы в выводе не дублировалась команда),
        # без PROMPT_COMMAND и приглашений.
        self._write(
            "stty -echo 2>/dev/null; unset PROMPT_COMMAND; "
            "PS1=''; PS2=''; printf '__TB_READY__\\n'\n"
        )
        deadline = time.monotonic() + 10
        buf = ""
        while "__TB_READY__" not in buf and time.monotonic() < deadline:
            buf += self._read(0.5)
        if "__TB_READY__" not in buf:
            raise RuntimeError("Шелл не запустился (не дождались приглашения)")

    # --- низкоуровневые операции с PTY ------------------------------------
    def _write(self, data: str) -> None:
        os.write(self.fd, data.encode("utf-8", errors="replace"))

    def _read(self, timeout: float) -> str:
        chunks: list[bytes] = []
        end = time.monotonic() + timeout
        while True:
            remaining = end - time.monotonic()
            if remaining <= 0:
                break
            r, _, _ = select.select([self.fd], [], [], remaining)
            if not r:
                break
            try:
                chunk = os.read(self.fd, 65536)
            except OSError as exc:
                if exc.errno == errno.EIO:
                    break
                raise
            if not chunk:
                break
            chunks.append(chunk)
        return b"".join(chunks).decode("utf-8", errors="replace")

    def _drain(self) -> None:
        while True:
            r, _, _ = select.select([self.fd], [], [], 0.2)
            if not r:
                return
            try:
                if not os.read(self.fd, 65536):
                    return
            except OSError:
                return

    # --- выполнение команды ------------------------------------------------
    def run(self, code: str, timeout: float | None = None) -> ExecResult:
        timeout = timeout or self.timeout
        started = time.monotonic()
        self._counter += 1
        mark_id = self._counter
        marker = f"__TB_MARK__{mark_id}"

        self._drain()
        payload = code.rstrip("\n") + "\n"
        # $? сохраняем СРАЗУ после кода, до любых других команд;
        # затем печатаем cwd и маркер с кодом возврата.
        payload += (
            "__tb_rc=$?; "
            f"printf '__TB_CWD__%s\\n' \"$PWD\"; "
            f"printf '{marker}:%s\\n' \"$__tb_rc\"\n"
        )
        self._write(payload)

        buf = ""
        timed_out = False
        returncode: int | None = None
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            buf += self._read(0.5)
            m = _MARK_RE.search(buf)
            if m and int(m.group(1)) == mark_id:
                returncode = int(m.group(2))
                break
            if not self._alive():
                break
        else:
            timed_out = True

        if timed_out:
            # Пытаемся прервать зависшую команду (флаги -y в промпте должны
            # были исключить интерактив, но подстрахуемся).
            self._write("\x03")
            buf += self._read(1.5)

        cwd = self.cwd
        cm = _CWD_RE.findall(buf)
        if cm:
            cwd = cm[-1].strip()
            self.cwd = cwd

        if not self._alive() and returncode is None:
            returncode = None
            # Перезапустим шелл для следующей команды.
            self._restart()

        output = _normalize_output(buf, self.strip_ansi)
        return ExecResult(
            command=code,
            output=output,
            returncode=returncode,
            timed_out=timed_out,
            duration=time.monotonic() - started,
            cwd=cwd,
        )

    def _alive(self) -> bool:
        try:
            pid, status = os.waitpid(self.pid, os.WNOHANG)
        except ChildProcessError:
            return False
        if pid == 0:
            return True
        return False

    def _restart(self) -> None:
        try:
            os.close(self.fd)
        except OSError:
            pass
        try:
            os.kill(self.pid, signal.SIGKILL)
        except (ProcessLookupError, PermissionError):
            pass
        self.start()

    def close(self) -> None:
        try:
            self._write("exit\n")
        except OSError:
            pass
        time.sleep(0.2)
        try:
            os.kill(self.pid, signal.SIGHUP)
        except (ProcessLookupError, PermissionError):
            pass
        try:
            os.close(self.fd)
        except OSError:
            pass


class _WinShell(ShellSession):
    """Windows: одна команда = один процесс PowerShell.

    Папка восстанавливается по маркеру после каждого вызова, так что `cd`
    между шагами работает. Переменные окружения между шагами НЕ живут —
    это сознательный компромисс ради работы без pywinpty.
    """

    def start(self) -> None:
        exe = self.shell_path or self._find_powershell()
        if not exe:
            raise RuntimeError(
                "PowerShell не найден. Укажите путь в shell.path конфига."
            )
        self.exe = exe
        os.makedirs(self.cwd, exist_ok=True)

    @staticmethod
    def _find_powershell() -> str | None:
        for cand in ("powershell.exe", "pwsh.exe"):
            found = None
            try:
                found = subprocess.check_output(
                    ["where.exe", cand], stderr=subprocess.DEVNULL, text=True
                ).splitlines()
            except (OSError, subprocess.CalledProcessError):
                continue
            if found:
                return found[0].strip()
        return None

    def run(self, code: str, timeout: float | None = None) -> ExecResult:
        timeout = timeout or self.timeout
        started = time.monotonic()
        self._counter += 1
        mark_id = self._counter

        # $LASTEXITCODE живёт только после нативных .exe; для командлетов
        # ориентируемся на $?.
        script = (
            f"Set-Location -LiteralPath '{self.cwd.replace(chr(39), chr(39) * 2)}'\n"
            f"{code}\n"
            "$tb_native = $LASTEXITCODE\n"
            "if ($null -ne $tb_native) { $tb_rc = $tb_native } "
            "else { $tb_rc = if ($?) { 0 } else { 1 } }\n"
            "Write-Output \"__TB_CWD__$($PWD.Path)\"\n"
            f"Write-Output \"__TB_MARK__{mark_id}:$tb_rc\"\n"
        )
        encoded = base64.b64encode(
            script.encode("utf-16-le", errors="replace")
        ).decode("ascii")

        timed_out = False
        try:
            proc = subprocess.run(
                [self.exe, "-NoProfile", "-NonInteractive",
                 "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded],
                capture_output=True,
                timeout=timeout,
                cwd=self.cwd,
            )
            raw = (proc.stdout or b"").decode("utf-8", errors="replace")
            err = (proc.stderr or b"").decode("utf-8", errors="replace")
            raw = (raw + ("\n" + err if err else ""))
            returncode = proc.returncode
        except subprocess.TimeoutExpired as exc:
            timed_out = True
            raw = (exc.stdout or b"").decode("utf-8", "replace") if isinstance(
                exc.stdout, bytes) else (exc.stdout or "")
            returncode = None

        m = _MARK_RE.findall(raw)
        if m:
            returncode = int(m[-1][1])
        cm = _CWD_RE.findall(raw)
        if cm:
            self.cwd = cm[-1].strip()

        return ExecResult(
            command=code,
            output=_normalize_output(raw, self.strip_ansi),
            returncode=returncode,
            timed_out=timed_out,
            duration=time.monotonic() - started,
            cwd=self.cwd,
        )
