# -*- coding: utf-8 -*-
"""
Build tools/data_slavic_460.py for UK, BE, PL
"""
import json

with open("tools/missing_460_en_ru.json", "r", encoding="utf-8") as f:
    base = json.load(f)

en_dict = base["en"]
ru_dict = base["ru"]

with open("tools/existing_translations.json", "r", encoding="utf-8") as f:
    existing = json.load(f)

# Helper function to generate Ukrainian translations based on Russian/English
def get_uk_dict():
    # Start with existing uk translations if any
    res = dict(existing.get("uk", {}))
    
    # Specific vocabulary mappings for Ukrainian
    # We map all 460 keys cleanly
    pass

print("Starting Slavic data builder...")
