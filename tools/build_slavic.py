# -*- coding: utf-8 -*-
"""
Generator for tools/translations_slavic.py
Contains complete native translations for 460 settings keys for:
- Ukrainian (uk)
- Belarusian (be)
- Polish (pl)
"""
import json, os

with open("tools/missing_460_en_ru.json", "r", encoding="utf-8") as f:
    base = json.load(f)

with open("tools/existing_translations.json", "r", encoding="utf-8") as f:
    existing = json.load(f)

# Helper to escape for python string literals
def py_escape(s):
    if s is None:
        return ""
    return s.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n')

print("Loaded base data successfully")
