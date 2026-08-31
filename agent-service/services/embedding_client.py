"""通义 text-embedding-v3 客户端 — DashScope OpenAI 兼容 /embeddings 接口"""

import logging
import httpx

from config import settings

log = logging.getLogger(__name__)


async def embed(text: str):
    """把文本编码为语义向量；失败返回 None。"""
    if not text or not text.strip():
        return None
    if not settings.EMBEDDING_API_KEY:
        log.error("EMBEDDING_API_KEY 未配置，无法调用向量化接口")
        return None
    try:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.post(
                settings.EMBEDDING_API_URL,
                headers={
                    "Authorization": f"Bearer {settings.EMBEDDING_API_KEY}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": settings.EMBEDDING_MODEL,
                    "input": text,
                    "dimensions": settings.EMBEDDING_DIM,
                    "encoding_format": "float",
                },
            )
            if resp.status_code != 200:
                log.error(f"Embedding API 错误: {resp.status_code} {resp.text[:200]}")
                return None
            data = resp.json()
            return data["data"][0]["embedding"]
    except Exception as e:
        log.error(f"Embedding 调用失败: {e}")
        return None
