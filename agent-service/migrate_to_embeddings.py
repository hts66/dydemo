"""一次性迁移：video_tags(128维标签向量) → video_embeddings(1024维语义向量)

复用旧库已有的 title + tags，只重新做 embedding，不重跑 qwen-vl 视觉分析（省额度）。
纯标准库实现（urllib + 线程池），无需 httpx，任意 Python3 环境可跑。

用法：python agent-service/migrate_to_embeddings.py
"""
import os, json, time, urllib.request, urllib.error
from concurrent.futures import ThreadPoolExecutor

QDRANT = "http://localhost:6333"
QKEY = "dyqdrant2024"
SRC = "video_tags"
DST = "video_embeddings"
EMB_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings"
EMB_KEY = os.getenv("EMBEDDING_API_KEY") or "sk-10e831e1ec3d4e48a85ed559499a79d9"
EMB_MODEL = "text-embedding-v3"
DIM = 1024
WORKERS = 8


def q_get_status(path):
    try:
        req = urllib.request.Request(QDRANT + path, headers={"api-key": QKEY})
        return urllib.request.urlopen(req, timeout=15).status
    except urllib.error.HTTPError as e:
        return e.code


def q_send(path, body, method):
    req = urllib.request.Request(QDRANT + path, data=json.dumps(body).encode(),
                                 headers={"api-key": QKEY, "Content-Type": "application/json"},
                                 method=method)
    return json.load(urllib.request.urlopen(req, timeout=60))


def ensure_dst():
    if q_get_status(f"/collections/{DST}") == 200:
        print(f"collection {DST} 已存在")
        return
    q_send(f"/collections/{DST}", {"vectors": {"size": DIM, "distance": "Cosine"}}, "PUT")
    print(f"已创建 collection {DST} ({DIM} 维, Cosine)")


def scroll_all():
    pts, offset = [], None
    while True:
        body = {"limit": 256, "with_payload": True, "with_vector": False}
        if offset is not None:
            body["offset"] = offset
        d = q_send(f"/collections/{SRC}/points/scroll", body, "POST")["result"]
        batch = d.get("points", [])
        pts.extend(batch)
        offset = d.get("next_page_offset")
        if not offset or not batch:
            break
    return pts


def build_text(payload):
    title = (payload.get("title") or "").strip()
    tags = payload.get("tags") or []
    parts = []
    if title:
        parts.append(title)
    if tags:
        parts.append(" ".join(tags))
    return " ".join(parts) or "视频"


def embed(text):
    body = json.dumps({"model": EMB_MODEL, "input": text, "dimensions": DIM,
                       "encoding_format": "float"}).encode()
    for attempt in range(3):
        try:
            req = urllib.request.Request(EMB_URL, data=body,
                                         headers={"Authorization": f"Bearer {EMB_KEY}",
                                                  "Content-Type": "application/json"}, method="POST")
            return json.load(urllib.request.urlopen(req, timeout=30))["data"][0]["embedding"]
        except urllib.error.HTTPError as e:
            if e.code == 429:
                time.sleep(1.5 * (attempt + 1))
                continue
            print("embed err", e.code)
            return None
        except Exception:
            time.sleep(1.0)
    return None


def process(p):
    vec = embed(build_text(p.get("payload", {})))
    if vec is None:
        return None
    return {"id": p["id"], "vector": vec, "payload": p.get("payload", {})}


def main():
    ensure_dst()
    pts = scroll_all()
    print(f"待迁移: {len(pts)} 条")
    t0 = time.time()
    results, done = [], 0
    with ThreadPoolExecutor(max_workers=WORKERS) as ex:
        for r in ex.map(process, pts):
            done += 1
            if r:
                results.append(r)
            if done % 100 == 0:
                print(f"[{done}/{len(pts)}] {done / (time.time() - t0):.1f}/s ok={len(results)}")
    for i in range(0, len(results), 100):
        q_send(f"/collections/{DST}/points?wait=true", {"points": results[i:i + 100]}, "PUT")
    print(f"完成: embed 成功 {len(results)}/{len(pts)}, 已写入 {DST}, 耗时 {time.time() - t0:.0f}s")


if __name__ == "__main__":
    main()
