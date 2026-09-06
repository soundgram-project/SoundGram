# -*- coding: utf-8 -*-
"""
Apply SoundGram translations to all res/values* directories in TMessagesProj.
Resolves AAPT2 collisions by:
1. Omitting keys from values/strings_soundgram.xml that already exist in values/strings.xml.
2. Removing keys from strings_nax.xml that are now managed in strings_soundgram.xml.
"""
import os, sys, re
import xml.etree.ElementTree as ET

from tools.data_en_ru import STRINGS_EN, STRINGS_RU
from tools.data_slavic import STRINGS_UK, STRINGS_BE, STRINGS_PL
from tools.data_western import STRINGS_DE, STRINGS_FR, STRINGS_ES, STRINGS_IT, STRINGS_PT, STRINGS_NL, STRINGS_NO
from tools.data_middle_east import STRINGS_TR, STRINGS_AR, STRINGS_FA, STRINGS_IW, STRINGS_AZ, STRINGS_UZ
from tools.data_asian import STRINGS_ID, STRINGS_JA, STRINGS_KO, STRINGS_ZH_CN, STRINGS_ZH_TW

RES_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "TMessagesProj", "src", "main", "res"))

FOLDER_MAP = {
    'values': STRINGS_EN,
    'values-ar': STRINGS_AR,
    'values-az': STRINGS_AZ,
    'values-be': STRINGS_BE,
    'values-de': STRINGS_DE,
    'values-es': STRINGS_ES,
    'values-es-rES': STRINGS_ES,
    'values-fa': STRINGS_FA,
    'values-fa-rIR': STRINGS_FA,
    'values-fr': STRINGS_FR,
    'values-id': STRINGS_ID,
    'values-in-rID': STRINGS_ID,
    'values-it': STRINGS_IT,
    'values-it-rIT': STRINGS_IT,
    'values-iw': STRINGS_IW,
    'values-ja': STRINGS_JA,
    'values-ja-rJP': STRINGS_JA,
    'values-ko': STRINGS_KO,
    'values-nl': STRINGS_NL,
    'values-no': STRINGS_NO,
    'values-pl': STRINGS_PL,
    'values-pl-rPL': STRINGS_PL,
    'values-pt-rBR': STRINGS_PT,
    'values-ru': STRINGS_RU,
    'values-ru-rRU': STRINGS_RU,
    'values-tr': STRINGS_TR,
    'values-tr-rTR': STRINGS_TR,
    'values-uk': STRINGS_UK,
    'values-uk-rUA': STRINGS_UK,
    'values-uz': STRINGS_UZ,
    'values-zh-rCN': STRINGS_ZH_CN,
    'values-zh-rTW': STRINGS_ZH_TW,
}

def escape_for_android_xml(val):
    # Unescape any existing XML entities first to normalize
    val = val.replace('&amp;', '&').replace('&quot;', '"').replace('&lt;', '<').replace('&gt;', '>')
    # Unescape any existing backslash-escaped quotes
    val = val.replace(r"\'", "'").replace(r'\"', '"')
    
    # XML escape
    val = val.replace('&', '&amp;')
    val = val.replace('<', '&lt;').replace('>', '&gt;')
    
    # Android resource escape
    val = val.replace("'", r"\'")
    val = val.replace('"', r'\"')
    return val

def clean_strings_nax(folder_path, soundgram_keys):
    nax_path = os.path.join(folder_path, "strings_nax.xml")
    if not os.path.exists(nax_path):
        return 0
    
    # Read raw text and remove matching <string name="..."> tags or parse with regex/DOM
    with open(nax_path, "r", encoding="utf-8") as f:
        lines = f.readlines()
    
    kept_lines = []
    removed_count = 0
    in_removed_block = False
    
    name_regex = re.compile(r'<string\s+name="([^"]+)"')
    end_regex = re.compile(r'</string>')
    
    for line in lines:
        if in_removed_block:
            if end_regex.search(line):
                in_removed_block = False
            continue
        
        m = name_regex.search(line)
        if m and m.group(1) in soundgram_keys:
            removed_count += 1
            if not end_regex.search(line):
                in_removed_block = True
            continue
        
        kept_lines.append(line)
    
    with open(nax_path, "w", encoding="utf-8") as f:
        f.writelines(kept_lines)
    
    return removed_count

def write_strings_soundgram(folder_path, lang_dict, is_base_values):
    sg_path = os.path.join(folder_path, "strings_soundgram.xml")
    
    # Check if any keys are in strings.xml or strings_nimarko.xml in this folder
    existing_strings_in_folder = set()
    for fname in ["strings.xml", "strings_nimarko.xml"]:
        other_xml = os.path.join(folder_path, fname)
        if os.path.exists(other_xml):
            try:
                tree = ET.parse(other_xml)
                for elem in tree.getroot().findall("string"):
                    name = elem.attrib.get("name")
                    if name:
                        existing_strings_in_folder.add(name)
            except Exception:
                pass
    
    lines = []
    lines.append('<?xml version="1.0" encoding="utf-8"?>\n')
    lines.append('<resources>\n')
    
    for k in STRINGS_EN.keys():
        # If this key is already defined in strings.xml in this folder, skip it to avoid duplicate
        if k in existing_strings_in_folder:
            continue
        
        val = lang_dict.get(k, STRINGS_EN[k])
        escaped_val = escape_for_android_xml(val)
        lines.append(f'    <string name="{k}">{escaped_val}</string>\n')
    
    lines.append('</resources>\n')
    
    with open(sg_path, "w", encoding="utf-8") as f:
        f.writelines(lines)
    
    return len(lines) - 3  # minus header, resources open & close

def run():
    all_sg_keys = set(STRINGS_EN.keys())
    print(f"Applying {len(all_sg_keys)} SoundGram string keys to {len(FOLDER_MAP)} folders...")
    
    for folder_name, lang_dict in FOLDER_MAP.items():
        folder_path = os.path.join(RES_DIR, folder_name)
        if not os.path.exists(folder_path):
            os.makedirs(folder_path, exist_ok=True)
            print(f"Created folder: {folder_name}")
        
        is_base = (folder_name == 'values')
        
        # 1. Clean strings_nax.xml if present
        removed = clean_strings_nax(folder_path, all_sg_keys)
        
        # 2. Write strings_soundgram.xml
        written = write_strings_soundgram(folder_path, lang_dict, is_base)
        
        print(f"[{folder_name}] written: {written} strings, cleaned from nax: {removed}")
    
    print("Translation application completed successfully!")

if __name__ == '__main__':
    run()
