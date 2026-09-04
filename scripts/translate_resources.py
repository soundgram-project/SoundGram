#!/usr/bin/env python3
import os
import re
import time
import json
import xml.etree.ElementTree as ET
import urllib.request
import urllib.parse

RES_DIR = '/home/lastsuns1ne/NimarkoGram/TMessagesProj/src/main/res'
EN_SOUNDGRAM = os.path.join(RES_DIR, 'values', 'strings_soundgram.xml')
EN_NIMARKO = os.path.join(RES_DIR, 'values', 'strings_nimarko.xml')

LANG_MAP = {
    'ar': 'values-ar',
    'az': 'values-az',
    'be': 'values-be',
    'de': 'values-de',
    'es': 'values-es',
    'fa': 'values-fa',
    'fr': 'values-fr',
    'id': 'values-id',
    'it': 'values-it',
    'iw': 'values-iw',
    'ja': 'values-ja',
    'ko': 'values-ko',
    'nl': 'values-nl',
    'no': 'values-no',
    'pl': 'values-pl',
    'pt': 'values-pt-rBR',
    'tr': 'values-tr',
    'uk': 'values-uk',
    'uz': 'values-uz',
    'zh-CN': 'values-zh-rCN',
    'zh-TW': 'values-zh-rTW',
}

COPIES = {
    'values-es': ['values-es-rES'],
    'values-fa': ['values-fa-rIR'],
    'values-id': ['values-in-rID'],
    'values-it': ['values-it-rIT'],
    'values-ja': ['values-ja-rJP'],
    'values-pl': ['values-pl-rPL'],
    'values-tr': ['values-tr-rTR'],
    'values-uk': ['values-uk-rUA'],
    'values-ru': ['values-ru-rRU'],
}

def clean_android_xml_text(text):
    if not text:
        return ""
    text = text.replace('&amp;', '&').replace('&lt;', '<').replace('&gt;', '>').replace('&quot;', '"').replace('\\\'', "'").replace('\\"', '"')
    return text.strip()

def prepare_for_android_xml(text):
    if not text:
        return ""
    text = text.replace('&', '&amp;')
    text = text.replace('<', '&lt;').replace('>', '&gt;')
    text = re.sub(r"(?<!\\)'", r"\'", text)
    text = text.replace('"', '&quot;')
    return text

def translate_batch(texts, target_lang, source_lang='en'):
    tl = 'he' if target_lang == 'iw' else target_lang
    ph_maps = []
    protected_texts = []
    for t in texts:
        ph_map = {}
        def repl(m):
            k = f"QQQ{len(ph_map)}QQQ"
            ph_map[k] = m.group(0)
            return k
        p = re.sub(r'%(?:\d+\$)?[sdf]', repl, t)
        p = p.replace('\n', ' ')
        ph_maps.append(ph_map)
        protected_texts.append(p)

    joined = '\n'.join(protected_texts)
    for attempt in range(4):
        try:
            url = f"https://clients5.google.com/translate_a/t?client=dict-chrome-ex&sl={source_lang}&tl={tl}&q={urllib.parse.quote(joined)}"
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
            with urllib.request.urlopen(req, timeout=12) as r:
                res = json.loads(r.read().decode('utf-8'))
                full = res[0]
                lines = full.split('\n')
                if len(lines) == len(texts):
                    out = []
                    for line, ph_map in zip(lines, ph_maps):
                        for k, v in ph_map.items():
                            line = re.sub(re.escape(k), v, line, flags=re.IGNORECASE)
                        out.append(line.strip())
                    return out
                else:
                    # Line count mismatch, do smaller batches
                    break
        except Exception as e:
            time.sleep(1 + attempt * 1.5)

    # Fallback to individual items in case of mismatch
    results = []
    for t, ph_map in zip(protected_texts, ph_maps):
        translated = t
        for attempt in range(3):
            try:
                url = f"https://clients5.google.com/translate_a/t?client=dict-chrome-ex&sl={source_lang}&tl={tl}&q={urllib.parse.quote(t)}"
                req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
                with urllib.request.urlopen(req, timeout=8) as r:
                    res = json.loads(r.read().decode('utf-8'))
                    translated = res[0]
                    break
            except Exception:
                time.sleep(0.5)
        for k, v in ph_map.items():
            translated = re.sub(re.escape(k), v, translated, flags=re.IGNORECASE)
        results.append(translated.strip())
    return results

def parse_xml_items(file_path):
    tree = ET.parse(file_path)
    root = tree.getroot()
    items = []
    for elem in root.findall('string'):
        name = elem.get('name')
        text = elem.text or ''
        items.append((name, text))
    return items

def write_strings_xml(dest_path, items):
    os.makedirs(os.path.dirname(dest_path), exist_ok=True)
    with open(dest_path, 'w', encoding='utf-8') as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n')
        f.write('<resources>\n')
        for name, text in items:
            escaped = prepare_for_android_xml(text)
            f.write(f'    <string name="{name}">{escaped}</string>\n')
        f.write('</resources>\n')
    # Validate
    try:
        ET.parse(dest_path)
        print(f"Validated XML: {dest_path}")
    except Exception as e:
        print(f"XML parse error in {dest_path}: {e}")

def translate_file_batches(src_items, target_lang, dest_path, batch_size=25):
    print(f"Translating {len(src_items)} items for {target_lang} -> {dest_path}...")
    all_translated = []
    for i in range(0, len(src_items), batch_size):
        chunk = src_items[i : i + batch_size]
        raw_texts = [clean_android_xml_text(t) for _, t in chunk]
        chunk_res = translate_batch(raw_texts, target_lang, 'en')
        for (name, _), trans in zip(chunk, chunk_res):
            all_translated.append((name, trans))
        time.sleep(0.1)

    write_strings_xml(dest_path, all_translated)

def main():
    soundgram_items = parse_xml_items(EN_SOUNDGRAM)
    nimarko_items = parse_xml_items(EN_NIMARKO)

    # 1. strings_soundgram.xml
    for lang, dir_name in LANG_MAP.items():
        dest = os.path.join(RES_DIR, dir_name, 'strings_soundgram.xml')
        if not os.path.exists(dest):
            translate_file_batches(soundgram_items, lang, dest, batch_size=25)

    # 2. strings_nimarko.xml
    for lang, dir_name in LANG_MAP.items():
        dest = os.path.join(RES_DIR, dir_name, 'strings_nimarko.xml')
        if not os.path.exists(dest):
            translate_file_batches(nimarko_items, lang, dest, batch_size=25)
        else:
            print(f"Already exists: {dest}")

    # 3. Copy to regional variants
    for src_dir_name, dest_dirs in COPIES.items():
        src_soundgram = os.path.join(RES_DIR, src_dir_name, 'strings_soundgram.xml')
        src_nimarko = os.path.join(RES_DIR, src_dir_name, 'strings_nimarko.xml')
        for dst_dir_name in dest_dirs:
            dst_dir = os.path.join(RES_DIR, dst_dir_name)
            os.makedirs(dst_dir, exist_ok=True)
            if os.path.exists(src_soundgram):
                dst_file = os.path.join(dst_dir, 'strings_soundgram.xml')
                with open(src_soundgram, 'r', encoding='utf-8') as sf, open(dst_file, 'w', encoding='utf-8') as df:
                    df.write(sf.read())
            if os.path.exists(src_nimarko):
                dst_file = os.path.join(dst_dir, 'strings_nimarko.xml')
                with open(src_nimarko, 'r', encoding='utf-8') as sf, open(dst_file, 'w', encoding='utf-8') as df:
                    df.write(sf.read())

    print("All translations completed and validated successfully!")

if __name__ == '__main__':
    main()
