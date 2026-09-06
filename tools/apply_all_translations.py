#!/usr/bin/env python3
import os
import glob
import json
import shutil
import html
import xml.etree.ElementTree as ET

RES_DIR = 'TMessagesProj/src/main/res'

# 1. Load all translations
all_translations = {}
for fname in [
    'tools/translations_slavic.json',
    'tools/translations_western.json',
    'tools/translations_middle_east.json',
    'tools/translations_asian.json'
]:
    with open(fname, 'r', encoding='utf-8') as f:
        data = json.load(f)
        all_translations.update(data)

print(f"Loaded translations for {len(all_translations)} languages: {list(all_translations.keys())}")

# Define NekoX and Neko keys
NAX_KEYS = {
    'DontSendReadMessagePackets', 'MessageSavingSaveMedia', 'SaveDeletedMessageForBotUser',
    'ClearMessageDatabase', 'EnableSaveEditsHistory', 'DontSendUploadProgress',
    'EnableSaveDeletedMessages', 'MessageSavingSaveMediaInPrivateChats', 'SaveDeletedMessageForBot',
    'MessageSavingSaveMediaHint', 'GhostEssentialsHeader', 'MessageSavingSaveMediaInPublicGroups',
    'MessageSavingSaveMediaInPrivateGroups', 'DontSendOnlinePackets', 'TranslucentDeletedMessages',
    'DontReadStoriesPackets', 'MessageSavingSaveMediaInPrivateChannels',
    'MessageSavingSaveMediaInPublicChannels', 'ClearMessageDatabaseNotification'
}

NEKO_KEYS = {
    'PasscodePanicCodeRemove'
}

# Ensure values-pt exists (mirrors values-pt-rBR if missing)
pt_br_dir = os.path.join(RES_DIR, 'values-pt-rBR')
pt_dir = os.path.join(RES_DIR, 'values-pt')
if not os.path.exists(pt_dir) and os.path.exists(pt_br_dir):
    shutil.copytree(pt_br_dir, pt_dir)
    print("Created values-pt from values-pt-rBR")

# Ensure values-zh exists (mirrors values-zh-rCN if missing)
zh_cn_dir = os.path.join(RES_DIR, 'values-zh-rCN')
zh_dir = os.path.join(RES_DIR, 'values-zh')
if not os.path.exists(zh_dir) and os.path.exists(zh_cn_dir):
    shutil.copytree(zh_cn_dir, zh_dir)
    print("Created values-zh from values-zh-rCN")

# Mappings of language key to resource directories
FOLDER_MAPPINGS = {
    'uk': ['values-uk', 'values-uk-rUA'],
    'be': ['values-be'],
    'pl': ['values-pl', 'values-pl-rPL'],
    'de': ['values-de'],
    'fr': ['values-fr'],
    'es': ['values-es', 'values-es-rES'],
    'it': ['values-it', 'values-it-rIT'],
    'pt-rBR': ['values-pt-rBR', 'values-pt'],
    'nl': ['values-nl'],
    'no': ['values-no'],
    'tr': ['values-tr', 'values-tr-rTR'],
    'ar': ['values-ar'],
    'fa': ['values-fa', 'values-fa-rIR'],
    'iw': ['values-iw'],
    'az': ['values-az'],
    'uz': ['values-uz'],
    'id': ['values-id', 'values-in-rID'],
    'ja': ['values-ja', 'values-ja-rJP'],
    'ko': ['values-ko'],
    'zh-rCN': ['values-zh-rCN', 'values-zh'],
    'zh-rTW': ['values-zh-rTW'],
}

def escape_android_xml(text):
    if not text:
        return ""
    text = html.unescape(text)
    text = text.replace("&", "&amp;")
    text = text.replace("<", "&lt;")
    text = text.replace(">", "&gt;")
    text = text.replace("'", r"\'")
    text = text.replace('"', r'\"')
    if text.startswith("@"):
        text = r"\@" + text[1:]
    elif text.startswith("?"):
        text = r"\?" + text[1:]
    return text

def get_existing_keys_in_folder(folder_path):
    keys = set()
    for xml_file in glob.glob(os.path.join(folder_path, '*.xml')):
        try:
            tree = ET.parse(xml_file)
            for child in tree.getroot():
                if child.tag == 'string':
                    name = child.attrib.get('name')
                    if name:
                        keys.add(name)
        except Exception as e:
            pass
    return keys

def append_keys_to_xml(file_path, keys_dict):
    if not keys_dict:
        return 0
    
    lines_to_add = []
    for k, v in keys_dict.items():
        escaped_val = escape_android_xml(v)
        lines_to_add.append(f'    <string name="{k}">{escaped_val}</string>')
    
    if os.path.exists(file_path):
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        idx = content.rfind('</resources>')
        if idx != -1:
            new_content = content[:idx] + '\n'.join(lines_to_add) + '\n' + content[idx:]
        else:
            new_content = content + '\n' + '\n'.join(lines_to_add) + '\n'
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
    else:
        new_content = '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n' + '\n'.join(lines_to_add) + '\n</resources>\n'
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
    return len(keys_dict)

total_added = 0
for lang, folders in FOLDER_MAPPINGS.items():
    lang_dict = all_translations.get(lang, {})
    if not lang_dict:
        print(f"WARNING: No translations found for {lang}!")
        continue
    
    for folder_name in folders:
        folder_path = os.path.join(RES_DIR, folder_name)
        os.makedirs(folder_path, exist_ok=True)
        
        existing_keys = get_existing_keys_in_folder(folder_path)
        
        # 1. NAX keys
        nax_to_add = {k: lang_dict[k] for k in NAX_KEYS if k in lang_dict and k not in existing_keys}
        added_nax = append_keys_to_xml(os.path.join(folder_path, 'strings_nax.xml'), nax_to_add)
        existing_keys.update(nax_to_add.keys())
        
        # 2. NEKO keys
        neko_to_add = {k: lang_dict[k] for k in NEKO_KEYS if k in lang_dict and k not in existing_keys}
        added_neko = append_keys_to_xml(os.path.join(folder_path, 'strings_neko.xml'), neko_to_add)
        existing_keys.update(neko_to_add.keys())
        
        # 3. Main strings.xml keys (the other ~440 keys)
        main_to_add = {k: lang_dict[k] for k in lang_dict if k not in NAX_KEYS and k not in NEKO_KEYS and k not in existing_keys}
        added_main = append_keys_to_xml(os.path.join(folder_path, 'strings.xml'), main_to_add)
        existing_keys.update(main_to_add.keys())
        
        folder_total = added_nax + added_neko + added_main
        total_added += folder_total
        print(f"Applied to {folder_name:16}: +{added_nax} nax, +{added_neko} neko, +{added_main} strings (total {len(existing_keys)} strings now)")

print(f"\nAll translations applied successfully! Total strings inserted across all folders: {total_added}")
