"""Разбор ответа ассистента: ищем блоки кода с меткой [EXECUTE].

Поддерживаемые форматы (ИИ иногда слегка отступает от спеки):

    ```bash
    # [EXECUTE]
    npm init -y
    ```

    ```
    [EXECUTE]
    docker compose up -d --build
    ```

    ```sh
    // [EXECUTE]   <- на случай виндового настроения
    winget install --silent Git.Git
    ```
"""

from __future__ import annotations

import re
from dataclasses import dataclass

# Ограждённый блок кода: ```язык\nтело\n```. Без DOTALL-апатии — тело между
# тремя бэктиками, без трёх бэктик внутри (ИИ так не вкладывает).
_FENCE_RE = re.compile(r"```[^\n`]*\n(.*?)```", re.DOTALL)

# Строка с меткой [EXECUTE], возможно закомментированная (#, //, <!--, /*)
_MARKER_RE = re.compile(
    r"^[ \t]*(?:(?://+|#+|<!--|/\*)\s*)?\[\s*EXECUTE\s*\]"
    r"(?:\s*-->|\s*\*/)?[ \t]*\r?\n?",
    re.IGNORECASE | re.MULTILINE,
)

# Метка где угодно в тексте (для «смягчающего» запасного варианта)
_MARKER_ANYWHERE_RE = re.compile(r"\[\s*EXECUTE\s*\]", re.IGNORECASE)


@dataclass(frozen=True)
class Command:
    """Команда, извлечённая из ответа ИИ."""

    code: str
    marker_in_block: bool


def _clean_block(body: str) -> str:
    """Удаляет строку(и) с меткой [EXECUTE] и лишние пустые строки по краям."""
    cleaned = _MARKER_RE.sub("", body)
    return cleaned.strip().strip("\n").strip()


def extract_commands(text: str) -> list[Command]:
    """Вернуть все исполняемые блоки из сообщения (по спеке — не больше одного).

    Порядок проверки:
      1. Блоки, внутри которых есть строка-метка [EXECUTE]  (основной формат);
      2. Если метка есть в сообщении, но ВНУТРИ блока её нет — берём блоки
         (ИИ мог написать метку перед блоком или в info-строке ```).
    """
    if not text:
        return []

    blocks = _FENCE_RE.findall(text)
    if not blocks:
        return []

    commands: list[Command] = []

    # 1. Канонический формат
    for body in blocks:
        if _MARKER_RE.search(body):
            code = _clean_block(body)
            if code:
                commands.append(Command(code=code, marker_in_block=True))

    if commands:
        return commands

    # 2. Смягчённый вариант: метка где-то рядом, блок(и) без явной метки.
    #    Берём только если блок ровно один — иначе неоднозначно.
    if _MARKER_ANYWHERE_RE.search(text) and len(blocks) == 1:
        code = _clean_block(blocks[0])
        if code:
            commands.append(Command(code=code, marker_in_block=False))

    return commands
