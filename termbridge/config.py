"""Загрузка конфигурации (YAML) с значениями по умолчанию."""

from __future__ import annotations

import os
from dataclasses import dataclass, field
from pathlib import Path

import yaml

from .executor import detect_os

DEFAULTS = {
    "cdp_url": "http://127.0.0.1:9222",
    "site": "auto",
    "tab_url_regex": "",
    "poll_interval": 1.2,
    "stable_polls": 2,
    "process_existing_on_start": False,
    "require_confirmation": False,
    "shell": {
        "os": "auto",
        "path": "",
        "cwd": ".",
        "command_timeout": 600,
        "strip_ansi": True,
    },
    "output": {
        "max_chars": 12000,
        "head_chars": 4000,
    },
    "browser": {
        "port": 9222,
        "profile_dir": "./.chrome-profile",
        "path": "",
    },
    "generic": {
        "assistant_selector": ".assistant",
        "composer_selector": "#composer",
        "submit": "button:#send-btn",
    },
}


def _deep_merge(base: dict, override: dict) -> dict:
    out = dict(base)
    for key, value in (override or {}).items():
        if isinstance(value, dict) and isinstance(out.get(key), dict):
            out[key] = _deep_merge(out[key], value)
        else:
            out[key] = value
    return out


@dataclass
class Config:
    raw: dict = field(default_factory=dict)
    path: Path | None = None

    # удобные аксессоры
    def __getitem__(self, key):
        return self.raw[key]

    def get(self, *keys, default=None):
        cur = self.raw
        for key in keys:
            if not isinstance(cur, dict) or key not in cur:
                return default
            cur = cur[key]
        return cur

    @property
    def shell_os(self) -> str:
        value = self.get("shell", "os", default="auto")
        return detect_os() if value in ("auto", "", None) else value

    @property
    def shell_cwd(self) -> str:
        cwd = self.get("shell", "cwd", default=".")
        if not os.path.isabs(cwd) and self.path is not None:
            cwd = str((self.path.parent / cwd).resolve())
        return cwd

    @property
    def profile_dir(self) -> str:
        d = self.get("browser", "profile_dir", default="./.chrome-profile")
        if not os.path.isabs(d) and self.path is not None:
            d = str((self.path.parent / d).resolve())
        return d


def load_config(path: str | None = None) -> Config:
    cfg_path: Path | None = None
    data: dict = {}
    if path:
        cfg_path = Path(path)
        if not cfg_path.exists():
            raise FileNotFoundError(f"Конфиг не найден: {path}")
        data = yaml.safe_load(cfg_path.read_text(encoding="utf-8")) or {}
    else:
        for candidate in ("config.yaml", "config.yml"):
            p = Path(candidate)
            if p.exists():
                cfg_path = p
                data = yaml.safe_load(p.read_text(encoding="utf-8")) or {}
                break
    merged = _deep_merge(DEFAULTS, data)
    return Config(raw=merged, path=cfg_path)
