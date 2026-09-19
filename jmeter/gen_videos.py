"""从后端 /api/works 抓取真实视频路径，生成 videos.csv 供 JMeter 使用。

需要后端 (localhost:8080) 在运行。用法: python gen_videos.py
"""
import json
import re
import urllib.request

OUT = "videos.csv"
TARGET = 50

paths = []
seen = set()
page = 1
while len(paths) < TARGET and page <= 10:
    url = f"http://localhost:8080/api/works?page={page}&size=10"
    try:
        with urllib.request.urlopen(url, timeout=10) as r:
            data = json.loads(r.read().decode("utf-8"))
        items = data.get("data") or []
        if not items:
            break
        for w in items:
            m = re.match(r"https?://[^/]+/dydemo/videos/(.+)", w.get("url") or "")
            if m and m.group(1) not in seen:
                seen.add(m.group(1))
                paths.append("/media/videos/" + m.group(1))
    except Exception as e:
        print(f"page {page} failed: {e}")
    page += 1

with open(OUT, "w", encoding="utf-8", newline="") as f:
    f.write("\n".join(paths[:TARGET]) + "\n")
print(f"written {min(len(paths), TARGET)} paths to {OUT}")
