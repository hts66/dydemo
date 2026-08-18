import json, os, sys, re
from datetime import datetime, date

sys.stdout.reconfigure(encoding='utf-8')

base = r"C:\Users\27036\.claude\projects\C--Users-27036-Desktop-dydemo"

all_typed_msgs = []

for fname in sorted(os.listdir(base)):
    if not fname.endswith('.jsonl'):
        continue
    fpath = os.path.join(base, fname)
    with open(fpath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                d = json.loads(line)
            except:
                continue

            if d.get('type') != 'user' or d.get('promptSource') != 'typed':
                continue

            ts = d.get('timestamp', '')
            if not ts:
                continue
            try:
                dt_obj = datetime.fromisoformat(ts.replace('Z', '+00:00'))
            except:
                continue

            content = ''
            msg = d.get('message', {})
            if isinstance(msg, dict):
                c = msg.get('content', '')
                if isinstance(c, str):
                    content = c
                elif isinstance(c, list):
                    parts = []
                    for item in c:
                        if isinstance(item, dict):
                            if item.get('type') == 'text':
                                parts.append(item.get('text', ''))
                            elif item.get('type') == 'image':
                                parts.append('[IMAGE]')
                    content = ' '.join(parts)

            if not content.strip():
                continue

            all_typed_msgs.append({
                'datetime': dt_obj,
                'content': content.strip(),
                'sessionId': d.get('sessionId', ''),
            })

all_typed_msgs.sort(key=lambda x: x['datetime'])


def desensitize(text):
    """Mask sensitive info - avoid backslash-heavy regex"""
    # Mask file paths by looking for known patterns
    # Replace any backslash paths starting with C:\ or similar
    text = text.replace('C:\\Users\\27036', 'C:\\Users\\***')
    text = text.replace('C:/Users/27036', 'C:/Users/***')

    # Mask UUIDs: 8-4-4-4-12 hex pattern
    p = re.compile(r'[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}')
    text = p.sub('<UUID>', text)

    # Mask emails
    p2 = re.compile(r'[\w.\-]+@[\w.\-]+\.\w+')
    text = p2.sub('***@***.***', text)

    # Mask image-cache paths
    p3 = re.compile(r'image-cache[/\\][^\s,;]+')
    text = p3.sub('image-cache/***', text)

    return text


target_days = [date(2026, 7, 17), date(2026, 7, 18)]

for day in target_days:
    msgs = [m for m in all_typed_msgs if m['datetime'].date() == day]

    export_path = os.path.join(base, f"batch_{day}.txt")
    with open(export_path, 'w', encoding='utf-8') as fout:
        sep = "=" * 110
        fout.write(sep + "\n")
        fout.write(f"  Vibe Coding - {day}  ({len(msgs)} messages)\n")
        fout.write(sep + "\n\n")

        for i, m in enumerate(msgs):
            ts = m['datetime'].strftime('%Y-%m-%d %H:%M:%S')
            content = desensitize(m['content'])

            fout.write(f"[{i+1:3d}] {ts}\n")
            fout.write(f"     Session: {m['sessionId'][:8]}...\n")
            fout.write(f"     {content}\n")
            fout.write("\n")

    print(f"{day}: {len(msgs)} messages -> {export_path}")

print("\nDONE.")
