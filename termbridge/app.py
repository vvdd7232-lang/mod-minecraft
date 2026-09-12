"""Главный цикл моста: опрос страницы -> команда -> шелл -> ответ в чат."""

from __future__ import annotations

import hashlib
import queue
import sys
import threading
import time
from pathlib import Path

from . import browser as browser_mod
from .adapters import Adapter, pick_adapter
from .config import Config
from .executor import ExecResult, ShellSession, os_human_name
from .parser import extract_commands

# Цвета (если терминал поддерживает)
class C:
    RESET = "\033[0m"
    DIM = "\033[2m"
    GREEN = "\033[32m"
    YELLOW = "\033[33m"
    RED = "\033[31m"
    CYAN = "\033[36m"
    BOLD = "\033[1m"


def _sig(text: str) -> str:
    return hashlib.sha1(text.encode("utf-8", errors="replace")).hexdigest()[:16]


def format_reply(command: str, result: ExecResult) -> str:
    """Текст, который отправится в чат как «вывод терминала»."""
    head = f"[ВЫВОД ТЕРМИНАЛА] код возврата: {result.returncode}"
    if result.timed_out:
        head = f"[ВЫВОД ТЕРМИНАЛА] ТАЙМАУТ ({result.duration:.0f} c) — команда прервана (Ctrl+C)"
    body = result.output or "(пустой вывод)"
    return f"{head}\n$ {command}\n\n{body}"


def truncate_output(text: str, max_chars: int, head_chars: int) -> tuple[str, bool]:
    if len(text) <= max_chars:
        return text, False
    tail_chars = max_chars - head_chars - 200
    note = (
        f"\n...[вывод обрезан мостом: всего {len(text)} символов, "
        f"показаны начало и конец]...\n"
    )
    return text[:head_chars] + note + text[-tail_chars:], True


class Bridge:
    def __init__(self, cfg: Config, *, auto_choose_tab: bool = False,
                 stdin_q: queue.Queue | None = None):
        self.cfg = cfg
        self.browser = browser_mod.Browser(cfg["cdp_url"])
        self.shell: ShellSession | None = None
        self.page = None
        self.adapter: Adapter | None = None
        self.seen: set[str] = set()           # сигнатуры обработанных ответов
        self.candidates: dict[str, int] = {}  # сигнатура -> сколько раз видна
        self.paused = False
        self.auto_choose_tab = auto_choose_tab
        self.stdin_q = stdin_q or queue.Queue()
        self.stop_event = threading.Event()

    # --- жизненный цикл ----------------------------------------------------
    def setup(self) -> None:
        self.browser.connect()
        self.page, self.adapter = self._select_page()
        self._snapshot_existing()

        shell_os = self.cfg.shell_os
        self.shell = ShellSession.create(
            shell_os,
            cwd=self.cfg.shell_cwd,
            timeout=float(self.cfg.get("shell", "command_timeout", default=600)),
            strip_ansi=bool(self.cfg.get("shell", "strip_ansi", default=True)),
            shell_path=self.cfg.get("shell", "path", default="") or None,
        )
        self.shell.start()
        print(f"{C.DIM}Терминал ({os_human_name(shell_os)}), cwd={self.shell.cwd}{C.RESET}")

    def _select_page(self):
        regex = self.cfg.get("tab_url_regex", default="") or ""
        site = self.cfg.get("site", default="auto")
        generic_cfg = self.cfg.raw.get("generic")

        page, candidates = self.browser.find_page(
            regex,
            pick_adapter("http://x", site, generic_cfg)[0] if site != "auto" else None,
        )
        if page is None and not regex and site == "auto":
            # перебираем вкладки и ищем любую с известным адаптером
            pages = self.browser.all_pages()
            for p in pages:
                adapter, matched = pick_adapter(p.url, "auto", generic_cfg)
                if adapter:
                    page, self.adapter = p, adapter
                    break
        else:
            if page is not None:
                self.adapter, _ = pick_adapter(page.url, site, generic_cfg)
                if self.adapter is None and site == "auto":
                    self.adapter = None

        if page is None:
            pages = self.browser.all_pages()
            print(f"{C.RED}Не нашёл подходящую вкладку чата.{C.RESET}")
            print("Открытые вкладки:")
            for i, p in enumerate(pages):
                print(f"  [{i}] {p.url}")
            print(
                "\nОткройте чат в запущенном браузере или укажите site/tab_url_regex "
                "в config.yaml."
            )
            raise SystemExit(3)
        if self.adapter is None:
            # неизвестный сайт, но вкладку выбрали — подключаем generic
            from .adapters import generic_adapter
            self.adapter = generic_adapter(generic_cfg)
            print(
                f"{C.YELLOW}Сайт не распознан ({page.url}), использую generic-адаптер "
                f"из конфига.{C.RESET}"
            )
        return page, self.adapter

    def _snapshot_existing(self) -> None:
        if self.cfg.get("process_existing_on_start", default=False):
            return
        try:
            messages = browser_mod.find_assistant_elements(self.page, self.adapter)
        except Exception:  # noqa: BLE001
            messages = []
        for _el, text in messages:
            self.seen.add(_sig(text))
            self.candidates[_sig(text)] = 99
        if messages:
            print(f"{C.DIM}{len(messages)} стар. ответ(ов) на странице пропущено.{C.RESET}")

    # --- основной цикл -----------------------------------------------------
    def run(self) -> int:
        interval = float(self.cfg.get("poll_interval", default=1.2))
        stable_need = int(self.cfg.get("stable_polls", default=2))
        while not self.stop_event.is_set():
            self._handle_stdin()
            if not self.paused:
                try:
                    self.tick(stable_need)
                except browser_mod.BrowserError as exc:
                    print(f"{C.YELLOW}[браузер] {exc}{C.RESET}")
                    self._reconnect_page()
                except Exception as exc:  # noqa: BLE001
                    print(f"{C.RED}[ошибка цикла] {exc!r}{C.RESET}")
            self.stop_event.wait(interval)
        return 0

    def tick(self, stable_need: int = 2) -> None:
        """Один проход опроса (вынесен наружу ради тестов)."""
        # вкладка могла быть закрыта/перезагружена
        if self.page is None or self.page.is_closed():
            self._reconnect_page()
            return

        messages = browser_mod.find_assistant_elements(self.page, self.adapter)
        alive_sigs = set()

        for _el, text in messages:
            sig = _sig(text)
            alive_sigs.add(sig)
            if sig in self.seen:
                continue
            self.candidates[sig] = self.candidates.get(sig, 0) + 1
            if self.candidates[sig] < stable_need:
                continue  # ответ ещё печатается / стабилизируется
            self.seen.add(sig)

            commands = extract_commands(text)
            if not commands:
                continue
            if len(commands) > 1:
                print(f"{C.YELLOW}В ответе {len(commands)} блоков [EXECUTE], "
                      f"по спеке выполняю первый.{C.RESET}")
            self._handle_command(commands[0].code, text_sig=sig)

        # чистим кандидатов исчезнувших ответов
        for dead in set(self.candidates) - alive_sigs:
            self.candidates.pop(dead, None)

    def _handle_command(self, code: str, text_sig: str) -> None:
        print(f"\n{C.BOLD}{C.GREEN}▶ [EXECUTE]{C.RESET} {C.DIM}(cwd: {self.shell.cwd}){C.RESET}")
        for line in code.splitlines():
            print(f"  {C.CYAN}{line}{C.RESET}")

        if self.cfg.get("require_confirmation", default=False):
            if not self._ask_yes_no("Выполнить команду? [y/N] "):
                print(f"{C.YELLOW}Пропущено пользователем.{C.RESET}")
                self._safe_send("Команда пропущена пользователем — не выполняй её "
                                "без повторной выдачи блока [EXECUTE].")
                return

        result = self.shell.run(code)
        status = (
            f"{C.GREEN}ok{C.RESET}" if result.ok
            else (f"{C.YELLOW}timeout{C.RESET}" if result.timed_out
                  else f"{C.RED}exit={result.returncode}{C.RESET}")
        )
        print(f"{C.DIM}→ {status}, {result.duration:.1f} c, "
              f"{len(result.output)} симв. вывода{C.RESET}")

        reply = format_reply(code, result)
        max_chars = int(self.cfg.get("output", "max_chars", default=12000))
        head_chars = int(self.cfg.get("output", "head_chars", default=4000))
        reply, truncated = truncate_output(reply, max_chars, head_chars)
        if truncated:
            print(f"{C.DIM}→ вывод обрезан до {max_chars} симв.{C.RESET}")

        self._safe_send(reply)

    def _safe_send(self, text: str) -> None:
        fallback = Path("termbridge_last_output.txt")
        try:
            browser_mod.send_message(self.page, self.adapter, text)
            print(f"{C.GREEN}✓ вывод отправлен в чат{C.RESET}\n")
        except Exception as exc:  # noqa: BLE001
            fallback.write_text(text, encoding="utf-8")
            print(f"{C.RED}✗ не смог отправить ответ в чат: {exc}{C.RESET}")
            print(f"{C.YELLOW}Текст сохранён в {fallback.name} — вставьте вручную."
                  f"{C.RESET}\n")

    # --- управление --------------------------------------------------------
    def _ask_yes_no(self, prompt: str) -> bool:
        print(prompt, end=" ", flush=True)
        answer = sys.stdin.readline().strip().lower()
        return answer in ("y", "yes", "д", "да")

    def _handle_stdin(self) -> None:
        while True:
            try:
                cmd = self.stdin_q.get_nowait()
            except queue.Empty:
                return
            if cmd in ("q", "quit", "exit", "й"):
                print(f"{C.YELLOW}Останавливаюсь...{C.RESET}")
                self.stop_event.set()
            elif cmd in ("p", "pause", "з"):
                self.paused = True
                print(f"{C.YELLOW}Пауза. 'r' — продолжить, q — выход.{C.RESET}")
            elif cmd in ("r", "resume", "c", "к"):
                self.paused = False
                print(f"{C.GREEN}Продолжаю работу.{C.RESET}")
            elif cmd in ("s", "skip", "ы"):
                self.candidates.clear()
                print(f"{C.YELLOW}Ожидающие ответы сброшены.{C.RESET}")
            elif cmd in ("h", "help", "р"):
                print("Команды: p — пауза, r — продолжить, "
                      "s — сбросить ожидание, q — выход")
            elif cmd:
                print(f"Неизвестная команда '{cmd}'. h — помощь.")

    def _reconnect_page(self) -> None:
        try:
            self.page, self.adapter = self._select_page()
            print(f"{C.GREEN}Вкладка переподключена.{C.RESET}")
        except SystemExit:
            raise
        except Exception as exc:  # noqa: BLE001
            print(f"{C.DIM}ожидаю вкладку... ({exc}){C.RESET}")
            time.sleep(2)

    def stop(self) -> None:
        self.stop_event.set()
        if self.shell:
            self.shell.close()
        self.browser.close()


def stdin_thread(q: queue.Queue, stop_event: threading.Event) -> None:
    """Читаем команды управления из терминала, пока работает мост."""
    while not stop_event.is_set():
        line = sys.stdin.readline()
        if line == "":
            time.sleep(0.3)
            continue
        q.put(line.strip().lower())
