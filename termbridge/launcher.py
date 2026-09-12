"""Запуск браузера пользователя с включённым CDP-портом.

Почему отдельный профиль:
* Chrome (с ~v136) запрещает remote-debugging для дефолтного профиля;
* нельзя открыть один и тот же профиль дважды — выделенный профиль моста
  не конфликтует с обычным браузером пользователя.

Войдите в свой ИИ-чат в этом окне один раз — куки сохранятся в профиле.
"""

from __future__ import annotations

import os
import shutil
import subprocess
import sys
from pathlib import Path

WINDOWS_CANDIDATES = [
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
    os.path.expandvars(r"%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe"),
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
]
MACOS_CANDIDATES = [
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
    "/Applications/Chromium.app/Contents/MacOS/Chromium",
]
NIX_CANDIDATES = [
    "google-chrome",
    "google-chrome-stable",
    "chromium",
    "chromium-browser",
    "microsoft-edge",
    "microsoft-edge-stable",
    "brave-browser",
]


def find_browser(explicit_path: str = "") -> str | None:
    if explicit_path:
        p = Path(os.path.expanduser(explicit_path))
        if p.exists():
            return str(p)
        if shutil.which(explicit_path):
            return explicit_path
        return None

    if sys.platform.startswith("win"):
        for cand in WINDOWS_CANDIDATES:
            if os.path.exists(cand):
                return cand
    elif sys.platform == "darwin":
        for cand in MACOS_CANDIDATES:
            if os.path.exists(cand):
                return cand
    for cand in NIX_CANDIDATES:
        found = shutil.which(cand)
        if found:
            return found
    return None


def launch_browser(port: int, profile_dir: str, explicit_path: str = "",
                   start_url: str = "") -> int:
    exe = find_browser(explicit_path)
    if not exe:
        print(
            "Браузер не найден автоматически. Укажите путь в config.yaml\n"
            "(browser.path) или запустите вручную, например:\n\n"
            '  chrome --remote-debugging-port=9222 '
            '--user-data-dir=./.chrome-profile\n',
            file=sys.stderr,
        )
        return 2

    Path(profile_dir).mkdir(parents=True, exist_ok=True)
    args = [
        exe,
        f"--remote-debugging-port={port}",
        f"--user-data-dir={os.path.abspath(profile_dir)}",
        "--no-first-run",
        "--no-default-browser-check",
    ]
    if start_url:
        args.append(start_url)

    # Отвязываемся от процесса терминала, чтобы браузер жил после выхода.
    kwargs = {"start_new_session": True}
    log = open(Path(profile_dir) / "browser.log", "ab")
    kwargs.update(stdout=log, stderr=log, stdin=subprocess.DEVNULL)

    subprocess.Popen(args, **kwargs)
    print(f"Браузер запущен: {exe}")
    print(f"CDP-порт: {port}, профиль: {os.path.abspath(profile_dir)}")
    print("Войдите в чат ИИ в этом окне, затем запустите: python -m termbridge run")
    return 0
