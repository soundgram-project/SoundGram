#!/usr/bin/env python3
import glob
import re

res_dir = 'TMessagesProj/src/main/res'

# 1. Fix CloudConfigSyncDate across all strings_neko.xml files
for f in sorted(glob.glob(res_dir + '/values*/strings_neko.xml')):
    with open(f, 'r', encoding='utf-8') as fp:
        content = fp.read()
    
    m = re.search(r'<string name="CloudConfigSyncDate">(.*?)</string>', content)
    if m:
        val = m.group(1)
        # normalize to %1$s and %2$s
        # handle %1 and %2 or %s and %s
        new_val = val
        if '%1$s' not in new_val:
            if '%1' in new_val and '%2' in new_val:
                new_val = new_val.replace('%1', '%1$s').replace('%2', '%2$s')
            elif new_val.count('%s') >= 2:
                parts = new_val.split('%s')
                new_val = parts[0] + '%1$s' + parts[1] + '%2$s' + ''.join(parts[2:])
        
        if new_val != val:
            new_tag = f'<string name="CloudConfigSyncDate">{new_val}</string>'
            content = content[:m.start()] + new_tag + content[m.end():]
            with open(f, 'w', encoding='utf-8') as fp:
                fp.write(content)
            print(f"Fixed {f}: {val} -> {new_val}")

# 2. Fix NoHandleAppInstalled in values-ru/strings.xml
ru_strings_path = f'{res_dir}/values-ru/strings.xml'
with open(ru_strings_path, 'r', encoding='utf-8') as fp:
    ru_content = fp.read()

m = re.search(r'<string name="NoHandleAppInstalled">(.*?)</string>', ru_content)
if m:
    old_val = m.group(1)
    new_val = "Нет приложения для открытия файлов типа «%1$s», установите подходящее"
    if old_val != new_val:
        new_tag = f'<string name="NoHandleAppInstalled">{new_val}</string>'
        ru_content = ru_content[:m.start()] + new_tag + ru_content[m.end():]
        with open(ru_strings_path, 'w', encoding='utf-8') as fp:
            fp.write(ru_content)
        print(f"Fixed values-ru NoHandleAppInstalled: {old_val} -> {new_val}")

print("Done fixing release strings.")
