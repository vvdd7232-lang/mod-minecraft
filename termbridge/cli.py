"""Командный интерфейс: run | launch | prompt | doctor."""

from __future__ import annotations

import argparse
import queue
import signal
import sys
import threading
from pathlib import Path

from . import __version__
from .adapters import pick_adapter
from .app import Bridge, stdin_thread, C
from .config import load_config
from .executor import ShellSession, os_human_name
from .launcher import launch_browser

REPO_ROOT = Path(__file__).resolve().parent.parent
PROMPT_FILE = REPO_ROOT / "prompts" / "system_prompt.md"

SITE_URLS = {
    "chatgpt": "https://chatgpt.com/",
    "claude": "https://claude.ai/new",
    "gemini": "https://gemini.google.com/app",
}


def _fix_console_utf8() -> None:
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
        except Exception:  # noqa: BLE001
            pass


def cmd_run(args: argparse.Namespace) -> int:
    cfg = load_config(args.config)
    print(f"{C.BOLD}termbridge v{__version__}{C.RESET} — мост «чат ИИ ⇄ терминал»")
    print(f"{C.DIM}Команды управления: p — пауза, r — продолжить, "
          f"s — сбросить ожидание, q — выход{C.RESET}\n")

    bridge = Bridge(cfg)
    bridge.setup()
    print(f"{C.GREEN}Подключено к: {bridge.page.url}{C.RESET}")
    print(f"Адаптер: {bridge.adapter.name}")
    print(f"{C.DIM}Жду ответы ИИ с блоками [EXECUTE]...{C.RESET}\n")

    q: queue.Queue = queue.Queue()
    stop = bridge.stop_event
    reader = threading.Thread(target=stdin_thread, args=(q, stop), daemon=True)
    bridge.stdin_q = q
    reader.start()

    def _sigint(_num, _frame):
        print(f"\n{C.YELLOW}Ctrl+C — завершаю...{C.RESET}")
        bridge.stop_event.set()

    signal.signal(signal.SIGINT, _sigint)
    try:
        rc = bridge.run()
    finally:
        bridge.stop()
    return rc


def cmd_launch(args: argparse.Namespace) -> int:
    cfg = load_config(args.config)
    port = int(cfg.get("browser", "port", default=9222))
    profile = cfg.get("browser", "profile_dir", default="./.chrome-profile")
    path = args.path or cfg.get("browser", "path", default="") or ""
    url = SITE_URLS.get(args.site, "")
    return launch_browser(port, profile, path, start_url=url)


def cmd_prompt(args: argparse.Namespace) -> int:
    cfg = load_config(args.config)
    tpl = PROMPT_FILE.read_text(encoding="utf-8")
    text = tpl.replace("{{OS}}", os_human_name(cfg.shell_os))
    if args.save:
        Path(args.save).write_text(text, encoding="utf-8")
        print(f"Промпт сохранён в {args.save}")
    else:
        print(text)
        print(f"\n{C.DIM}--- Отправьте этот текст первым сообщением в чат ИИ. "
              f"Сохранить в файл: python -m termbridge prompt --save prompt.txt{C.RESET}")
    return 0


def cmd_doctor(args: argparse.Namespace) -> int:
    cfg = load_config(args.config)
    ok = True

    # 1. Шелл
    print(f"{C.BOLD}[1/3] Терминал ({os_human_name(cfg.shell_os)}){C.RESET}")
    session = None
    try:
        session = ShellSession.create(
            cfg.shell_os,
            cwd=cfg.shell_cwd,
            timeout=30,
            strip_ansi=True,
            shell_path=cfg.get("shell", "path", default="") or None,
        )
        session.start()
        res = session.run("echo termbridge-doctor-ok")
        if "termbridge-doctor-ok" in res.output and res.returncode == 0:
            print(f"  {C.GREEN}✓ шелл работает, cwd={res.cwd}{C.RESET}")
        else:
            print(f"  {C.RED}✓ странный вывод: rc={res.returncode}, "
                  f"out={res.output!r}{C.RESET}")
            ok = False
    except Exception as exc:  # noqa: BLE001
        print(f"  {C.RED}✗ шелл не запустился: {exc}{C.RESET}")
        ok = False
    finally:
        if session:
            session.close()

    # 2. CDP
    print(f"{C.BOLD}[2/3] Подключение к браузеру ({cfg['cdp_url']}){C.RESET}")
    from . import browser as browser_mod
    from .browser import find_assistant_elements, _first_visible  # noqa: PLC2701

    br = browser_mod.Browser(cfg["cdp_url"])
    try:
        br.connect()
        pages = br.all_pages()
        print(f"  {C.GREEN}✓ CDP доступен, открыто вкладок: {len(pages)}{C.RESET}")
    except Exception as exc:  # noqa: BLE001
        print(f"  {C.RED}✗ {exc}{C.RESET}")
        return 1

    # 3. Адаптеры/селекторы на вкладках
    print(f"{C.BOLD}[3/3] Распознавание чата на вкладках{C.RESET}")
    site = cfg.get("site", default="auto")
    any_chat = False
    for page in pages:
        adapter, matched = pick_adapter(page.url, site, cfg.raw.get("generic"))
        if adapter is None and site == "auto":
            print(f"  {C.DIM}- {page.url[:90]} — не распознан{C.RESET}")
            continue
        if adapter is None:
            adapter = pick_adapter(page.url, "generic", cfg.raw.get("generic"))[0]
        any_chat = True
        try:
            msgs = find_assistant_elements(page, adapter)
            composer = _first_visible(page, adapter.composer_selectors, 1200)
            print(f"  {C.GREEN}✓ {adapter.name}{C.RESET} — {page.url[:80]}")
            print(f"      ответов на странице: {len(msgs)}, "
                  f"поле ввода: {'найдено' if composer else 'НЕ найдено'}")
            if composer is None:
                ok = False
        except Exception as exc:  # noqa: BLE001
            print(f"  {C.RED}✗ {page.url}: {exc}{C.RESET}")
            ok = False

    if not any_chat:
        print(f"  {C.YELLOW}! Ни одна вкладка не похожа на чат. "
              f"Откройте чат в запущенном браузере или настройте generic.{C.RESET}")
        ok = False

    br.close_connection_only()
    print(("\n" + (f"{C.GREEN}ВСЁ ОК{C.RESET}" if ok else
                   f"{C.YELLOW}Есть замечания (см. выше){C.RESET}")))
    return 0 if ok else 1


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="termbridge",
        description="Мост между веб-чатом ИИ и локальным терминалом.",
    )
    p.add_argument("--config", "-c", help="путь к config.yaml")
    p.add_argument("--version", action="version", version=f"termbridge {__version__}")
    sub = p.add_subparsers(dest="command", required=True)

    # --config доступен и до, и после подкоманды
    common = argparse.ArgumentParser(add_help=False)
    common.add_argument("--config", "-c", help="путь к config.yaml")

    sp = sub.add_parser("run", parents=[common],
                        help="запустить мост (опрос чата и выполнение команд)")
    sp.set_defaults(func=cmd_run)

    sp = sub.add_parser("launch", parents=[common],
                        help="запустить браузер с CDP-портом")
    sp.add_argument("--site", choices=list(SITE_URLS), default="",
                    help="сразу открыть нужный чат")
    sp.add_argument("--path", default="", help="путь к браузеру")
    sp.set_defaults(func=cmd_launch)

    sp = sub.add_parser("prompt", parents=[common],
                        help="вывести системный промпт для ИИ")
    sp.add_argument("--save", default="", help="сохранить промпт в файл")
    sp.set_defaults(func=cmd_prompt)

    sp = sub.add_parser("doctor", parents=[common],
                        help="проверить шелл, браузер и селекторы")
    sp.set_defaults(func=cmd_doctor)
    return p


def main(argv: list[str] | None = None) -> int:
    _fix_console_utf8()
    args = build_parser().parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
