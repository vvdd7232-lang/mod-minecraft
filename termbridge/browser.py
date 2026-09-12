"""Работа с браузером по протоколу CDP (Chrome DevTools Protocol).

Браузер должен быть запущен с --remote-debugging-port=9222 (см. launcher.py).
Playwright подключается к УЖЕ открытому браузеру пользователя, где тот залогинен
в свой чат — мы не управляем учётками и не тащим куки.
"""

from __future__ import annotations

import re
import time

from playwright.sync_api import (
    ElementHandle,
    Page,
    Playwright,
    TimeoutError as PWTimeoutError,
    sync_playwright,
)

from .adapters import Adapter


class BrowserError(RuntimeError):
    pass


class Browser:
    def __init__(self, cdp_url: str):
        self.cdp_url = cdp_url
        self._pw: Playwright | None = None
        self._browser = None

    # --- подключение -------------------------------------------------------
    def connect(self) -> None:
        self._pw = sync_playwright().start()
        try:
            self._browser = self._pw.chromium.connect_over_cdp(self.cdp_url)
        except Exception as exc:  # noqa: BLE001
            raise BrowserError(
                f"Не удалось подключиться к браузеру по {self.cdp_url}: {exc}\n"
                "Запустите браузер командой: python -m termbridge launch"
            ) from exc
        if not self._browser.contexts:
            raise BrowserError("В браузере нет открытых контекстов/окон.")

    def close(self) -> None:
        try:
            if self._browser is not None:
                # close() у CDP-подключения не закрывает сам браузер пользователя,
                # только рвёт наш коннект — но подстрахуемся явной остановкой.
                self.close_connection_only()
        finally:
            if self._pw is not None:
                self._pw.stop()
                self._pw = None

    def close_connection_only(self) -> None:
        try:
            if self._browser is not None:
                self._browser.close()
        except Exception:  # noqa: BLE001
            pass
        self._browser = None

    # --- вкладки -----------------------------------------------------------
    def all_pages(self) -> list[Page]:
        pages: list[Page] = []
        for ctx in self._browser.contexts:
            for page in ctx.pages:
                try:
                    _ = page.url
                    pages.append(page)
                except Exception:  # noqa: BLE001
                    continue
        return pages

    def find_page(self, url_regex: str = "", adapter: Adapter | None = None
                  ) -> tuple[Page | None, list[Page]]:
        """Найти вкладку чата. Возвращает (страница, кандидаты)."""
        pages = self.all_pages()
        if url_regex:
            rx = re.compile(url_regex, re.IGNORECASE)
            hits = [p for p in pages if rx.search(p.url)]
            return (hits[0] if hits else None), hits
        if adapter is not None:
            hits = [p for p in pages if adapter.matches_url(p.url)]
            if hits:
                return hits[0], hits
            # generic подходит под всё — берём первую не-служебную вкладку
            if adapter.key == "generic":
                real = [p for p in pages if not p.url.startswith(
                    ("chrome://", "devtools://", "edge://", "about:"))]
                if real:
                    return real[0], real
        return None, pages


# --------------------------------------------------------------------------
# Операции на странице
# --------------------------------------------------------------------------

def _first_visible(page: Page, selectors: list[str], timeout_ms: int = 1500
                   ) -> ElementHandle | None:
    deadline = time.time() + timeout_ms / 1000
    while time.time() < deadline:
        for sel in selectors:
            try:
                for el in page.query_selector_all(sel):
                    try:
                        if el.is_visible():
                            return el
                    except Exception:  # noqa: BLE001
                        continue
            except Exception:  # noqa: BLE001
                continue
        time.sleep(0.15)
    return None


def find_assistant_elements(page: Page, adapter: Adapter
                            ) -> list[tuple[ElementHandle, str]]:
    """Список (элемент, текст) ответов ассистента в порядке DOM."""
    result: list[tuple[ElementHandle, str]] = []
    for sel in adapter.assistant_selectors:
        try:
            els = page.query_selector_all(sel)
        except Exception:  # noqa: BLE001
            continue
        for el in els:
            try:
                if not el.is_visible():
                    continue
                text = (el.inner_text() or "").strip()
            except Exception:  # noqa: BLE001
                continue
            if text:
                result.append((el, text))
        if result:
            break
    return result


def read_composer_text(page: Page, adapter: Adapter) -> str:
    box = _first_visible(page, adapter.composer_selectors, timeout_ms=500)
    if box is None:
        return ""
    try:
        tag = box.evaluate("el => el.tagName")
        if str(tag).lower() == "textarea":
            return box.input_value() or ""
        return box.inner_text() or ""
    except Exception:  # noqa: BLE001
        return ""


def send_message(page: Page, adapter: Adapter, text: str) -> None:
    """Ввести текст в поле чата и отправить его."""
    box = _first_visible(page, adapter.composer_selectors, timeout_ms=5000)
    if box is None:
        raise BrowserError(
            f"Поле ввода не найдено (селекторы: {adapter.composer_selectors})"
        )

    box.scroll_into_view_if_needed(timeout=2000)
    box.click(timeout=2000)
    # select-all + delete — на случай остатков черновика
    page.keyboard.press("Control+A")
    page.keyboard.press("Delete")

    # insert_text отправляет именно «вставку текста» (как пасту), корректно
    # работает и с ProseMirror/contenteditable, и с textarea.
    page.keyboard.insert_text(text)

    # Убеждаемся, что текст реально лёг в поле.
    entered = read_composer_text(page, adapter)
    if not entered:
        # запасной путь для обычных textarea
        try:
            box.fill(text, timeout=2000)
        except Exception:  # noqa: BLE001
            raise BrowserError("Не удалось ввести текст в поле чата")

    _submit(page, adapter)


def _submit(page: Page, adapter: Adapter) -> None:
    if not adapter.enter_to_send:
        for sel in adapter.send_button_selectors:
            try:
                btn = page.query_selector(sel)
                if btn and btn.is_visible() and btn.is_enabled():
                    btn.click(timeout=2000)
                    return
            except Exception:  # noqa: BLE001
                continue
    # По умолчанию (и как фолбэк для кнопочных адаптеров) — Enter.
    try:
        page.keyboard.press("Enter")
    except PWTimeoutError:
        raise BrowserError("Не удалось нажать отправку (Enter)")
