"""Qdrant 向量数据库客户端 — 封装 collection 管理 + 向量搜索"""

import json
from typing import List, Dict, Any, Optional
import httpx
import logging

from config import settings
from services.tag_vocabulary import VECTOR_SIZE

log = logging.getLogger(__name__)


class QdrantClient:
    """基于 Qdrant REST API 的轻量客户端"""

    def __init__(self):
        self.base_url = f"http://{settings.QDRANT_HOST}:{settings.QDRANT_PORT}"
        self.collection = settings.QDRANT_COLLECTION
        self.headers: Dict[str, str] = {}
        if settings.QDRANT_API_KEY:
            self.headers["api-key"] = settings.QDRANT_API_KEY

    async def ensure_collection(self) -> None:
        """确保 collection 存在，不存在则创建"""
        async with httpx.AsyncClient(timeout=10) as client:
            url = f"{self.base_url}/collections/{self.collection}"
            resp = await client.get(url, headers=self.headers)

            if resp.status_code == 200:
                log.info(f"Qdrant collection '{self.collection}' 已存在")
                return

            # 创建 collection: 128维, Cosine 距离
            body = {
                "vectors": {
                    "size": VECTOR_SIZE,
                    "distance": "Cosine",
                }
            }
            put_resp = await client.put(url, headers={**self.headers, "Content-Type": "application/json"}, json=body)
            log.info(f"创建 Qdrant collection '{self.collection}': {put_resp.status_code} {put_resp.text}")
            if put_resp.status_code != 200:
                log.warning(f"Qdrant collection 创建可能失败: {put_resp.text}")

    async def search_similar(
        self, vector: List[float], limit: int = 5,
        filter_obj: dict = None, offset: int = 0
    ) -> List[Dict[str, Any]]:
        """搜索与给定向量最相似的点，返回 [{id, score, payload}]"""
        body = {
            "vector": vector,
            "limit": limit,
            "offset": offset,
            "with_payload": True,
        }
        if filter_obj:
            body["filter"] = filter_obj

        async with httpx.AsyncClient(timeout=15) as client:
            url = f"{self.base_url}/collections/{self.collection}/points/search"
            resp = await client.post(
                url,
                headers={**self.headers, "Content-Type": "application/json"},
                json=body,
            )

            if resp.status_code != 200:
                log.error(f"Qdrant 搜索失败: {resp.status_code} {resp.text}")
                return []

            data = resp.json()
            results = []
            for pt in data.get("result", []):
                payload = pt.get("payload", {})
                # Qdrant payload 值可能是嵌套结构，这里做扁平化
                flat_payload: Dict[str, Any] = {}
                if isinstance(payload, dict):
                    for k, v in payload.items():
                        flat_payload[k] = v
                results.append({
                    "id": pt.get("id"),
                    "score": pt.get("score", 0.0),
                    "payload": flat_payload,
                })

            return results

    async def upsert_points(self, points: List[Dict[str, Any]]) -> None:
        """批量 upsert 点"""
        if not points:
            return

        body = {"points": points}

        async with httpx.AsyncClient(timeout=30) as client:
            url = f"{self.base_url}/collections/{self.collection}/points?wait=true"
            resp = await client.put(
                url,
                headers={**self.headers, "Content-Type": "application/json"},
                json=body,
            )
            if resp.status_code == 200:
                log.info(f"Qdrant upsert {len(points)} 个向量成功")
            else:
                log.error(f"Qdrant upsert 失败: {resp.status_code} {resp.text}")

    async def get_processed_ids(self) -> set:
        """获取所有已处理的 point ID"""
        body = {"limit": 10000, "with_payload": False, "with_vector": False}

        async with httpx.AsyncClient(timeout=30) as client:
            url = f"{self.base_url}/collections/{self.collection}/points/scroll"
            resp = await client.post(
                url,
                headers={**self.headers, "Content-Type": "application/json"},
                json=body,
            )

            if resp.status_code != 200:
                return set()

            data = resp.json()
            ids = set()
            for pt in data.get("result", {}).get("points", []):
                if "id" in pt:
                    ids.add(pt["id"])
            return ids


# 全局单例
qdrant_client = QdrantClient()
