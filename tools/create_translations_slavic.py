# -*- coding: utf-8 -*-
"""
Construct tools/translations_slavic.py for UK, BE, PL
"""
import json, os

with open("tools/missing_460_en_ru.json", "r", encoding="utf-8") as f:
    base = json.load(f)

with open("tools/existing_translations.json", "r", encoding="utf-8") as f:
    existing = json.load(f)

en = base["en"]
ru = base["ru"]

# We will generate comprehensive dictionaries for uk, be, pl
# Using existing if available, plus mapped translations
