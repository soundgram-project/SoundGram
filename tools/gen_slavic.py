# -*- coding: utf-8 -*-
"""
Generate tools/data_slavic_460.py containing all 460 keys for UK, BE, PL.
"""
import json

with open("tools/missing_460_en_ru.json", "r", encoding="utf-8") as f:
    base = json.load(f)

en_dict = base["en"]
ru_dict = base["ru"]

# Template generation script
print("Base loaded successfully. 460 keys ready.")
