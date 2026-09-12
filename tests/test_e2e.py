"""Сквозной тест моста на мок-странице чата.

Эмулируем ответы ИИ через window.addAssistant(...), прогоняем Bridge.tick()
и проверяем, что команда выполнена, а вывод отправлен в чат.
"""

from __future__ import annotations

import tempfile
import time
from pathlib import Path

import pytest

from termbridge.app import Bridge
from termbridge.config import Config
from tests._chrome_ctl import find_chrome, launch_chromium

MOCK_URL = (Path(__file__).parent / "mock_chat" / "index.html").resolve().as_uri()

pytestmark = pytest.mark.skipif(find_chrome() is None,
                                reason="Тестовый Chromium не найден")


@pytest.fixture(scope="module")
def chrome():
    profile = Path(tempfile.mkdtemp(prefix="tb-chrome-"))
    proc, port = launch_chromium(profile, MOCK_URL)
    yield proc, port
    proc.terminate()
    try:
        proc.wait(timeout=5)
    except Exception:  # noqa: BLE001
        proc.kill()


def _make_bridge(cdp_url: str, cwd: Path) -> Bridge:
    cfg = Config(raw={
        "cdp_url": cdp_url,
        "site": "generic",
        "tab_url_regex": "",
        "poll_interval": 0.2,
        "stable_polls": 2,
        "process_existing_on_start": True,
        "require_confirmation": False,
        "shell": {
            "os": "auto", "path": "", "cwd": str(cwd),
            "command_timeout": 20, "strip_ansi": True,
        },
        "output": {"max_chars": 12000, "head_chars": 4000},
        "browser": {"port": 9222, "profile_dir": str(cwd / "profile"), "path": ""},
        "generic": {
            "assistant_selector": ".assistant",
            "composer_selector": "#composer",
            "submit": "button:#send-btn",
        },
    })
    bridge = Bridge(cfg)
    bridge.setup()
    return bridge


def _wait_until(fn, timeout=15, interval=0.2):
    deadline = time.time() + timeout
    last = None
    while time.time() < deadline:
        try:
            value = fn()
            if value:
                return value
        except Exception as exc:  # noqa: BLE001
            last = exc
        time.sleep(interval)
    raise AssertionError(f"Условие не дождались: {last}")


def test_command_executed_and_output_sent(chrome, tmp_path):
    proc, port = chrome
    bridge = _make_bridge(f"http://127.0.0.1:{port}", tmp_path)
    page = bridge.page
    assert "mock_chat" in page.url
    try:
        # 1. Обычная успешная команда
        msg1 = (
            "Сейчас выполню проверку.\n"
            "```bash\n"
            "# [EXECUTE]\n"
            "echo e2e-works-123 && mkdir -p project/src\n"
            "```\n"
            "Жду вывод."
        )
        page.evaluate("(t) => window.addAssistant(t)", msg1)
        _wait_until(lambda: (bridge.tick(2),
                             page.evaluate("() => window.sentMessages.length"))[1] >= 1)
        reply = page.evaluate(
            "() => window.sentMessages[window.sentMessages.length-1]")
        assert "ВЫВОД ТЕРМИНАЛА" in reply
        assert "e2e-works-123" in reply
        assert "код возврата: 0" in reply
        assert (tmp_path / "project" / "src").is_dir()

        # 2. Команда с ненулевым кодом возврата
        msg2 = (
            "Команда с ошибкой:\n"
            "```bash\n# [EXECUTE]\necho before-fail && false\n```"
        )
        page.evaluate("(t) => window.addAssistant(t)", msg2)
        _wait_until(lambda: (bridge.tick(2),
                             page.evaluate("() => window.sentMessages.length"))[1] >= 2)
        reply2 = page.evaluate(
            "() => window.sentMessages[window.sentMessages.length-1]")
        assert "before-fail" in reply2
        assert "код возврата: 1" in reply2

        # 3. cd должен сохраняться между шагами (постоянная сессия)
        msg3 = (
            "Захожу в папку:\n"
            "```bash\n# [EXECUTE]\ncd project/src && pwd\n```"
        )
        page.evaluate("(t) => window.addAssistant(t)", msg3)
        _wait_until(lambda: (bridge.tick(2),
                             page.evaluate("() => window.sentMessages.length"))[1] >= 3)
        reply3 = page.evaluate(
            "() => window.sentMessages[window.sentMessages.length-1]")
        assert "project" in reply3 and "src" in reply3

        # 4. Ответ без [EXECUTE] игнорируется
        before = page.evaluate("() => window.sentMessages.length")
        page.evaluate("(t) => window.addAssistant(t)", "Просто рассуждаю, команд нет.")
        for _ in range(4):
            bridge.tick(2)
            time.sleep(0.1)
        assert page.evaluate("() => window.sentMessages.length") == before
    finally:
        bridge.shell.close()
        bridge.browser.close_connection_only()
