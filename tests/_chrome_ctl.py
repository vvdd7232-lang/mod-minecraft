"""Запуск локального Chromium для e2e-тестов (CDP).

По умолчанию используется бинарь, скачанный в tools/ (см. tests/README);
можно указать свой через переменную окружения TERMBRIDGE_TEST_CHROME.
"""

from __future__ import annotations

import os
import socket
import subprocess
import time
import urllib.request
from pathlib import Path

import pytest

REPO = Path(__file__).resolve().parent.parent
DEFAULT_CHROME = REPO / "tools" / "sparticuz" / "package" / "bin" / "chromium"


def free_port() -> int:
    s = socket.socket()
    s.bind(("127.0.0.1", 0))
    port = s.getsockname()[1]
    s.close()
    return port


def find_chrome() -> str | None:
    env = os.environ.get("TERMBRIDGE_TEST_CHROME")
    if env and Path(env).exists():
        return env
    if DEFAULT_CHROME.exists():
        return str(DEFAULT_CHROME)
    return None


def launch_chromium(profile_dir: Path, url: str):
    """Запустить headless Chromium с CDP. Возвращает (Popen, port)."""
    exe = find_chrome()
    if not exe:
        pytest.skip("Тестовый Chromium не найден (см. tests/README)")

    base = Path(exe).parent
    env = os.environ.copy()
    libdir = f"{base}/al2023/lib:{base}/swiftshader"
    env["LD_LIBRARY_PATH"] = (
        f"{libdir}:{env['LD_LIBRARY_PATH']}" if env.get("LD_LIBRARY_PATH") else libdir
    )
    env["VK_ICD_FILENAMES"] = f"{base}/swiftshader/vk_swiftshader_icd.json"

    port = free_port()
    args = [
        exe,
        "--headless=new",
        "--no-sandbox",
        "--disable-dev-shm-usage",
        "--disable-gpu",
        "--no-zygote",
        "--single-process",
        "--enable-unsafe-swiftshader",
        f"--remote-debugging-port={port}",
        f"--user-data-dir={str(profile_dir)}",
        url,
    ]
    proc = subprocess.Popen(
        args, env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
    )
    deadline = time.time() + 30
    while time.time() < deadline:
        try:
            urllib.request.urlopen(f"http://127.0.0.1:{port}/json/version", timeout=1)
            return proc, port
        except Exception:  # noqa: BLE001
            if proc.poll() is not None:
                pytest.skip("Тестовый Chromium не стартовал в этом окружении")
            time.sleep(0.5)
    proc.kill()
    pytest.skip("CDP-порт Chromium не поднялся")
