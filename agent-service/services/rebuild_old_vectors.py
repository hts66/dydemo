"""旧视频向量重建 — 封面图 + 标题优先 + Vision补充 + Qdrant"""
import asyncio, json, base64, time, re
import httpx, pymysql

VISION_API_KEY = "sk-10e831e1ec3d4e48a85ed559499a79d9"
VISION_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
VISION_MODEL = "qwen-vl-plus"
QDRANT_URL = "http://localhost:6333"
QDRANT_KEY = "dyqdrant2024"
COLLECTION = "video_tags"
CONCURRENCY = 15

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

TITLE_TAGS = ["JK制服","连衣裙","旗袍","汉服","吊带","衬衫","T恤","西装",
              "短裙","长裙","牛仔裤","瑜伽裤","长裤",
              "黑丝","白丝","渔网","长袜","单人舞","多人舞",
              "御姐","甜妹","长发","短发","眼镜妹","苗条","少女感",
              "室内","室外","橘猫","英短","美短","布偶猫","暹罗猫","田园猫",
              "幼猫","成年猫","玩耍","睡觉","吃饭","卖萌","舔毛",
              "自然风光","城市夜景","海景","山景","日落","雪景","森林","花海","古镇","草原","湖景","瀑布","白天","夜晚"]

VISION_PROMPT = """观察这张封面图，判断视频内容并选择标签，只返回JSON数组。

可选标签:
大类(必选1): 美女 小猫 风景
上装(选1): 吊带 衬衫 T恤 西装
下装(选1): 短裙 长裙 牛仔裤 瑜伽裤 长裤
套装(选0-1): JK制服 连衣裙 旗袍 汉服
丝袜(选1): 黑丝 白丝 渔网 长袜
舞蹈(选0-1): 单人舞 多人舞
人物(选1-2): 御姐 甜妹 长发 短发 眼镜妹 苗条 少女感
场景(选0-1): 室内 室外

规则: 认真看穿搭。区分丝袜颜色。不确定不选。只返回数组。
示例: ["美女","短裙","黑丝","甜妹","长发","室内"]"""


def extract_title_tags(title):
    text = re.sub(r'#', '', (title or ""))
    return [t for t in TITLE_TAGS if t in text]


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
    for t in tags:
        if t in {"美女","小猫","风景"}: return tags
    for t in tags:
        d = TAG_DIMS.get(t)
        if d and 3 <= d <= 32: tags.insert(0, "美女"); return tags
        if d and 33 <= d <= 52: tags.insert(0, "小猫"); return tags
        if d and 53 <= d <= 66: tags.insert(0, "风景"); return tags
    return tags


async def process_one(wid, thumb, title, sem, client):
    async with sem:
        try:
            # 1. 标题标签（高置信度）
            title_tags = extract_title_tags(title)
            # 2. 封面图 Vision
            vision_tags = []
            if thumb and thumb.startswith("http"):
                img = await client.get(thumb, timeout=15)
                if img.status_code == 200 and len(img.content) > 100:
                    b64 = base64.b64encode(img.content).decode()
                    content = [{"type":"text","text":VISION_PROMPT},{"type":"image_url","image_url":{"url":f"data:image/jpeg;base64,{b64}"}}]
                    resp = await client.post(VISION_API_URL,
                        headers={"Content-Type":"application/json","Authorization":f"Bearer {VISION_API_KEY}"},
                        json={"model":VISION_MODEL,"messages":[{"role":"user","content":content}],"temperature":0.3,"max_tokens":1024}, timeout=90)
                    vision_tags = parse_tags(resp.json()["choices"][0]["message"]["content"])
            # 3. 合并
            merged = list(title_tags)
            for t in vision_tags:
                if t not in merged: merged.append(t)
            merged = ensure_category(merged)
            if not merged: return {"id": wid, "status": "no_tags"}
            # 4. 写 Qdrant
            vec = build_vector(merged)
            await client.put(
                f"{QDRANT_URL}/collections/{COLLECTION}/points?wait=true",
                headers={"api-key":QDRANT_KEY,"Content-Type":"application/json"},
                json={"points":[{"id":wid,"vector":vec,"payload":{"title":title,"tags":merged}}]}, timeout=30)
            return {"id": wid, "tags": merged, "status": "ok"}
        except Exception as e:
            return {"id": wid, "status": "error", "err": str(e)[:80]}


async def main():
    conn = pymysql.connect(host="localhost", user="hts", password="HTshun666!", database="dy", charset="utf8mb4")
    cur = conn.cursor()
    cur.execute("SELECT id, thumbnail, title FROM works WHERE thumbnail IS NOT NULL AND thumbnail!=''")
    works = [(r[0], r[1], r[2] or "") for r in cur.fetchall()]
    cur.close(); conn.close()

    # Check Qdrant for already-processed IDs
    resp = httpx.post(f"{QDRANT_URL}/collections/{COLLECTION}/points/scroll",
        headers={"api-key":QDRANT_KEY,"Content-Type":"application/json"},
        json={"limit":10000,"with_payload":False,"with_vector":False})
    done = set()
    if resp.status_code == 200:
        for pt in resp.json().get("result",{}).get("points",[]): done.add(pt["id"])
    works = [(w[0],w[1],w[2]) for w in works if w[0] not in done]
    print(f"待处理(跳过Qdrant已有): {len(works)}")
    sem = asyncio.Semaphore(CONCURRENCY)
    t0 = time.time(); cnt = [0]; ok = [0]
    async with httpx.AsyncClient(timeout=30) as client:
        async def w(*a):
            r = await process_one(*a, sem, client)
            cnt[0] += 1; ok[0] += (r["status"]=="ok")
            e = time.time()-t0; rate = cnt[0]/e if e>0 else 0
            print(f"[{cnt[0]}/{len(works)}] wid={r['id']} {r['status']} {';'.join(r.get('tags',[]))[:50]} | {rate:.1f}/s")
            return r
        await asyncio.gather(*[w(wid, t, ti) for wid, t, ti in works])
    print(f"done: {ok[0]}/{len(works)} time:{time.time()-t0:.0f}s")

asyncio.run(main())
