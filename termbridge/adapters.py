"""Адаптеры веб-чатов: как найти ответы ассистента, поле ввода и кнопку отправки.

Селекторы могут меняться у вендоров — поэтому у каждого адапера несколько
запасных вариантов, а для произвольного сайта есть generic-адаптер,
настраиваемый из config.yaml.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field


@dataclass
class Adapter:
    key: str
    name: str
    url_patterns: list[re.Pattern]
    assistant_selectors: list[str]
    composer_selectors: list[str]
    send_button_selectors: list[str] = field(default_factory=list)
    # True — после вставки текста жмём Enter; False — кликаем кнопку,
    # но если кнопку найти не удалось, всё равно пробуем Enter.
    enter_to_send: bool = True

    def matches_url(self, url: str) -> bool:
        return any(p.search(url) for p in self.url_patterns)


_R = lambda s: re.compile(s, re.IGNORECASE)

CHATGPT = Adapter(
    key="chatgpt",
    name="ChatGPT (chatgpt.com)",
    url_patterns=[_R(r"https?://(?:[\w-]+\.)?(?:chatgpt\.com|openai\.com)/")],
    assistant_selectors=[
        '[data-message-author-role="assistant"]',
        '[data-testid^="conversation-turn"] [data-message-author-role="assistant"]',
        ".markdown.prose",
    ],
    composer_selectors=[
        "#prompt-textarea",
        'div[contenteditable="true"][data-placeholder]',
        "div.ProseMirror[contenteditable='true']",
    ],
    send_button_selectors=[
        'button[data-testid="send-button"]',
        'button[aria-label*="Send" i]',
    ],
)

CLAUDE = Adapter(
    key="claude",
    name="Claude (claude.ai)",
    url_patterns=[_R(r"https?://(?:[\w-]+\.)?claude\.ai/")],
    assistant_selectors=[
        "div.font-claude-message",
        '[class*="font-claude-message"]',
        '[data-testid="message-content"][class*="claude"]',
        ".message-assistant .prose",
    ],
    composer_selectors=[
        'div[contenteditable="true"][role="textbox"]',
        "div.ProseMirror[contenteditable='true']",
        'div[contenteditable="true"][data-placeholder]',
        'div[contenteditable="true"]',
    ],
    send_button_selectors=[
        'button[aria-label*="Send" i]',
        'button[type="submit"]',
    ],
)

GEMINI = Adapter(
    key="gemini",
    name="Gemini (gemini.google.com)",
    url_patterns=[_R(r"https?://(?:[\w-]+\.)?gemini\.google\.com/")],
    assistant_selectors=[
        ".model-response-text",
        ".model-response-message .markdown-main-panel",
        "message-content.model-response-message",
    ],
    composer_selectors=[
        "div.richtextarea [contenteditable='true']",
        'div[aria-multiline="true"][contenteditable="true"]',
        "div.richtextarea textarea",
        'div[contenteditable="true"]',
    ],
    send_button_selectors=[
        'button[aria-label*="Send" i]',
        "button.send-button",
    ],
)

KNOWN_ADAPTERS = {a.key: a for a in (CHATGPT, CLAUDE, GEMINI)}


def generic_adapter(cfg: dict | None) -> Adapter:
    """Адаптер для произвольного сайта по селекторам из конфига."""
    cfg = cfg or {}

    class _Submit:  # разбор "enter" / "button:#send"
        mode = "enter"
        selector = ""

    raw = str(cfg.get("submit", "enter")).strip()
    send_buttons: list[str] = []
    enter = True
    if raw.startswith("button:"):
        send_buttons = [raw.split("button:", 1)[1].strip()]
        enter = False

    return Adapter(
        key="generic",
        name=f"Универсальный ({cfg.get('assistant_selector', '?')})",
        url_patterns=[_R(r".*")],
        assistant_selectors=[cfg.get("assistant_selector", ".assistant")],
        composer_selectors=[cfg.get("composer_selector", "#composer")],
        send_button_selectors=send_buttons,
        enter_to_send=enter,
    )


def pick_adapter(url: str, site: str = "auto", generic_cfg: dict | None = None
                 ) -> tuple[Adapter | None, list[Adapter]]:
    """По URL и настройке site вернуть (выбранный адаптер, все совпавшие)."""
    if site in KNOWN_ADAPTERS:
        a = KNOWN_ADAPTERS[site]
        return a, ([a] if a.matches_url(url) else [])
    if site == "generic":
        g = generic_adapter(generic_cfg)
        return g, [g]
    matched = [a for a in KNOWN_ADAPTERS.values() if a.matches_url(url)]
    if len(matched) == 1:
        return matched[0], matched
    return (None, matched)
