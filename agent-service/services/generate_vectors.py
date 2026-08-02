"""多帧视频向量生成 — 本地视频抽5帧 + 标题优先 + Vision补充 + Qdrant"""
import asyncio, json, os, sys, base64, subprocess, tempfile, time, shutil, re
from pathlib import Path
import httpx, pymysql

VIDEO_DIR = Path(r"C:\Users\27036\Desktop\dydemo\dyqianduan\douyin_downloads")
VISION_API_KEY = "sk-10e831e1ec3d4e48a85ed559499a79d9"
VISION_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
VISION_MODEL = "qwen-vl-plus"
QDRANT_URL = "http://localhost:6333"
QDRANT_KEY = "dyqdrant2024"
COLLECTION = "video_tags"
CONCURRENCY = 10

TAG_DIMS = {
    "美女":0,"小猫":1,"风景":2,
    "吊带":3,"衬衫":4,"T恤":5,"西装":6,
    "短裙":7,"长裙":8,"牛仔裤":9,"瑜伽裤":10,"长裤":11,
    "JK制服":12,"连衣裙":13,"旗袍":14,"汉服":15,
    "黑丝":18,"白丝":19,"渔网":20,"长袜":21,
    "单人舞":22,"多人舞":23,
    "御姐":24,"甜妹":25,"长发":26,"短发":27,"眼镜妹":28,"苗条":29,"少女感":30,
    "室内":31,"室外":32,
    "橘猫":33,"英短":34,"美短":35,"布偶猫":36,"暹罗猫":37,"田园猫":38,"其他猫":39,
    "橘色":40,"白色":41,"黑色":42,"灰色":43,"三花":44,"狸花":45,
    "幼猫":46,"成年猫":47,"玩耍":48,"睡觉":49,"吃饭":50,"卖萌":51,"舔毛":52,
    "自然风光":53,"城市夜景":54,"海景":55,"山景":56,"日落":57,"雪景":58,
    "森林":59,"花海":60,"古镇":61,"草原":62,"湖景":63,"瀑布":64,"白天":65,"夜晚":66,
}

VISION_PROMPT = """你是视频内容分析专家。观察这5张视频帧，判断内容并选择标签，只返回JSON数组。

可选标签:
大类(必选1个): 美女 小猫 风景
上装(选1个): 吊带 衬衫 T恤 西装
下装(选1个): 短裙 长裙 牛仔裤 瑜伽裤 长裤
套装(选0-1个): JK制服 连衣裙 旗袍 汉服
丝袜(选1个): 黑丝 白丝 渔网 长袜
舞蹈(选0-1个): 单人舞 多人舞
人物(选1-2个): 御姐 甜妹 长发 短发 眼镜妹 苗条 少女感
场景(选0-1): 室内 室外

规则: 仔细看每帧穿搭。重点区分丝袜颜色(黑/白)。不确定不选。只返回数组。

输出示例:
["美女","吊带","短裙","黑丝","单人舞","甜妹","长发","室内"]
["美女","连衣裙","白丝","御姐","长发","苗条","室内"]
["小猫","橘猫","玩耍","室内"]"""

TITLE_TAGS = ["JK制服","连衣裙","旗袍","汉服","吊带","衬衫","T恤","西装",
              "短裙","长裙","牛仔裤","瑜伽裤","长裤",
              "黑丝","白丝","渔网","长袜","单人舞","多人舞",
              "御姐","甜妹","长发","短发","眼镜妹","苗条","少女感",
              "室内","室外","橘猫","英短","美短","布偶猫","暹罗猫","田园猫",
              "幼猫","成年猫","玩耍","睡觉","吃饭","卖萌","舔毛",
              "自然风光","城市夜景","海景","山景","日落","雪景","森林","花海","古镇","草原","湖景","瀑布","白天","夜晚"]


def extract_title_tags(title, desc):
    """从标题/描述中提取标签（高置信度）"""
    text = f"{title} {desc}"
    found = []
    # 去掉#标记后匹配
    clean = re.sub(r'#', '', text)
    for tag in TITLE_TAGS:
        if tag in clean and tag not in found:
            found.append(tag)
    return found


def extract_frames(video_path, tmpdir):
    result = subprocess.run(["ffprobe","-v","error","-show_entries","format=duration","-of","csv=p=0",str(video_path)], capture_output=True, text=True, timeout=10)
    duration = float(result.stdout.strip())
    frames = []
    for pct in [0, 0.25, 0.50, 0.75, 0.95]:
        seek = min(duration * pct, max(0, duration - 0.5))
        out = os.path.join(tmpdir, f"f{pct}.jpg")
        subprocess.run(["ffmpeg","-y","-hide_banner","-loglevel","error","-ss",str(seek),"-i",str(video_path),"-frames:v","1","-q:v","3",out], capture_output=True, timeout=10)
        if os.path.exists(out) and os.path.getsize(out) > 100:
            frames.append(out)
    return frames


def build_vector(tags):
    vec = [0.0] * 128
    for t in tags:
        if t in TAG_DIMS: vec[TAG_DIMS[t]] = 1.0
    norm = sum(v*v for v in vec) ** 0.5
    return [v/norm for v in vec] if norm > 0 else vec


def parse_tags(text):
    try:
        t = text.strip()
        if t.startswith("```"): s=t.find("["); e=t.rfind("]"); t = t[s:e+1] if s>=0 and e>s else t
        tags = json.loads(t)
        return [x for x in tags if x in TAG_DIMS] if isinstance(tags, list) else []
    except: return []


def ensure_category(tags):
    """确保有大类标签"""
    cats = {"美女","小猫","风景"}
    for t in tags:
        if t in cats: return tags
    for t in tags:
        d = TAG_DIMS.get(t)
        if d and 3 <= d <= 32: tags.insert(0, "美女"); return tags
        if d and 33 <= d <= 52: tags.insert(0, "小猫"); return tags
        if d and 53 <= d <= 66: tags.insert(0, "风景"); return tags
    return tags


async def process_one(vid, wid, title, desc, sem, client):
    async with sem:
        mp4 = VIDEO_DIR / f"{vid}.mp4"
        if not mp4.exists(): return {"id": wid, "status": "no_file"}
        tmpdir = tempfile.mkdtemp()
        try:
            # 1. 标题标签（高置信度）
            title_tags = extract_title_tags(title, desc)
            # 2. 抽帧 + Vision
            loop = asyncio.get_event_loop()
            frames = await loop.run_in_executor(None, extract_frames, mp4, tmpdir)
            vision_tags = []
            if len(frames) >= 2:
                b64s = []
                for fp in frames:
                    with open(fp,"rb") as f: b64s.append(base64.b64encode(f.read()).decode())
                content = [{"type":"text","text":VISION_PROMPT}]
                for b in b64s: content.append({"type":"image_url","image_url":{"url":f"data:image/jpeg;base64,{b}"}})
                resp = await client.post(VISION_API_URL,
                    headers={"Content-Type":"application/json","Authorization":f"Bearer {VISION_API_KEY}"},
                    json={"model":VISION_MODEL,"messages":[{"role":"user","content":content}],"temperature":0.3,"max_tokens":1024}, timeout=90)
                data = resp.json()
                vision_tags = parse_tags(data["choices"][0]["message"]["content"])
            # 3. 合并: 标题优先 + Vision补充
            merged = list(title_tags)
            for t in vision_tags:
                if t not in merged: merged.append(t)
            merged = ensure_category(merged)
            if not merged: return {"id": wid, "status": "no_tags"}
            # 4. 写入 Qdrant
            vec = build_vector(merged)
            await client.put(
                f"{QDRANT_URL}/collections/{COLLECTION}/points?wait=true",
                headers={"api-key":QDRANT_KEY,"Content-Type":"application/json"},
                json={"points":[{"id":wid,"vector":vec,"payload":{"title":title,"tags":merged}}]}, timeout=30)
            return {"id": wid, "tags": merged, "status": "ok"}
        except Exception as e:
            return {"id": wid, "status": "error", "err": str(e)[:80]}
        finally:
            shutil.rmtree(tmpdir, ignore_errors=True)


async def main():
    with open(VIDEO_DIR / "oss_urls.json", "r", encoding="utf-8") as f: videos = json.load(f)
    conn = pymysql.connect(host="localhost", user="hts", password="HTshun666!", database="dy", charset="utf8mb4")
    cur = conn.cursor()
    cur.execute("SELECT id, url FROM works WHERE user_id=13")
    url2wid = {r[1]: r[0] for r in cur.fetchall()}
    cur.close(); conn.close()
    tasks = [(v["videoId"], url2wid[v["videoUrl"]], v.get("title",""), v.get("title","")) for v in videos if v["videoUrl"] in url2wid]
    print(f"待处理: {len(tasks)}")
    sem = asyncio.Semaphore(CONCURRENCY)
    t0 = time.time(); cnt = [0]; ok = [0]
    async with httpx.AsyncClient(timeout=30) as client:
        async def w(vid, wid, t, d):
            r = await process_one(vid, wid, t, d, sem, client)
            cnt[0] += 1; ok[0] += (r["status"]=="ok")
            e = time.time()-t0; rate = cnt[0]/e if e>0 else 0
            print(f"[{cnt[0]}/{len(tasks)}] wid={r['id']} {r['status']} {';'.join(r.get('tags',[]))[:60]} | {rate:.1f}/s")
            return r
        await asyncio.gather(*[w(v,i,t,d) for v,i,t,d in tasks])
    print(f"done: {ok[0]}/{len(tasks)} err:{len(tasks)-ok[0]} time:{time.time()-t0:.0f}s")

asyncio.run(main())
