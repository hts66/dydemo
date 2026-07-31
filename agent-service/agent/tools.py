"""LangChain 工具定义 — Agent 可调用的能力"""

import json
import logging
from typing import List, Optional
import httpx

from langchain_core.tools import tool

from config import settings
from services.tag_vocabulary import build_vector, all_tags_for_prompt
from services.qdrant_client import qdrant_client

log = logging.getLogger(__name__)


@tool
async def recommend_videos(query: str, limit: int = 5) -> str:
    """
    根据用户的自然语言描述推荐视频。当用户说"推荐XX视频"、"给我找XX"、"有没有XX"等时调用此工具。
    内部会先用AI提取标签，再通过向量搜索匹配最相似的视频。

    Args:
        query: 用户的原始查询语句，例如"推荐御姐黑丝长腿视频"
        limit: 返回视频数量，默认5个

    Returns:
        JSON格式的推荐结果，包含视频列表
    """
    try:
        # 1. 用 DeepSeek 提取标签
        tags = await _extract_tags(query)
        if not tags:
            return json.dumps({
                "found": False,
                "message": '抱歉，我没太理解你想看什么类型的视频，可以再说具体一点吗？比如"推荐御姐黑丝视频"~',
                "videos": []
            }, ensure_ascii=False)

        log.info(f"标签提取: '{query}' → {tags}")

        # 2. 构建向量
        vector = build_vector(tags)
        if all(v == 0.0 for v in vector):
            return json.dumps({
                "found": False,
                "message": "抱歉，我暂时没有收录这类视频的标签，换个类型试试？",
                "videos": []
            }, ensure_ascii=False)

        # 3. 搜索 Qdrant
        results = await qdrant_client.search_similar(vector, limit)
        if not results:
            return json.dumps({
                "found": False,
                "message": f"没有找到匹配'{', '.join(tags)}'的视频，换个关键词试试吧~",
                "videos": []
            }, ensure_ascii=False)

        # 4. 格式化为 JSON 返回给 LLM
        videos = []
        for r in results:
            payload = r.get("payload", {})
            videos.append({
                "id": r["id"],
                "score": round(r["score"], 2),
                "title": payload.get("title", ""),
                "description": payload.get("description", ""),
                "tags": payload.get("tags", []),
            })

        tag_str = "、".join(tags)
        message = f"为你找到了 {len(videos)} 个「{tag_str}」视频，快来看看吧~ 🎬"

        return json.dumps({
            "found": True,
            "message": message,
            "tags": tags,
            "videos": videos,
        }, ensure_ascii=False)

    except Exception as e:
        log.error(f"视频推荐失败: {e}")
        return json.dumps({
            "found": False,
            "message": "推荐系统暂时出了点小问题，稍后再试吧~",
            "videos": []
        }, ensure_ascii=False)


@tool
async def search_videos_by_keyword(keyword: str, limit: int = 5) -> str:
    """
    通过关键词搜索视频（标题/描述匹配）。当用户想搜索特定内容的视频时调用。

    Args:
        keyword: 搜索关键词
        limit: 返回数量，默认5个

    Returns:
        JSON格式的搜索结果
    """
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            resp = await client.get(
                f"{settings.SPRING_BOOT_API_URL}/api/works",
                params={"keyword": keyword, "page": 1, "size": limit},
            )
            if resp.status_code != 200:
                return json.dumps({"found": False, "message": "搜索服务暂时不可用", "videos": []}, ensure_ascii=False)

            data = resp.json()
            if data.get("code") != 200:
                return json.dumps({"found": False, "message": "搜索失败", "videos": []}, ensure_ascii=False)

            works = data["data"].get("records", [])
            videos = [
                {
                    "id": w["id"],
                    "title": w.get("title", ""),
                    "description": w.get("description", ""),
                    "username": w.get("username", "匿名用户"),
                    "url": w.get("url", ""),
                    "thumbnail": w.get("thumbnail", ""),
                }
                for w in works
            ]

            return json.dumps({
                "found": True,
                "message": f"找到 {len(videos)} 个包含「{keyword}」的视频",
                "videos": videos,
            }, ensure_ascii=False)

    except Exception as e:
        log.error(f"关键词搜索失败: {e}")
        return json.dumps({"found": False, "message": "搜索暂时不可用", "videos": []}, ensure_ascii=False)


@tool
async def get_hot_videos(limit: int = 5) -> str:
    """
    获取热门/热门推荐视频。当用户说"热门"、"有什么好看的"、"最近流行什么"等时调用。

    Args:
        limit: 返回数量，默认5个

    Returns:
        JSON格式的热门视频列表
    """
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            resp = await client.get(
                f"{settings.SPRING_BOOT_API_URL}/api/works/hot",
                params={"page": 1, "size": limit},
            )
            if resp.status_code != 200:
                return json.dumps({"found": False, "message": "暂时获取不到热门视频", "videos": []}, ensure_ascii=False)

            data = resp.json()
            if data.get("code") != 200:
                return json.dumps({"found": False, "message": "获取热门失败", "videos": []}, ensure_ascii=False)

            works = data["data"].get("records", [])
            videos = [
                {
                    "id": w["id"],
                    "title": w.get("title", ""),
                    "description": w.get("description", ""),
                    "username": w.get("username", "匿名用户"),
                    "url": w.get("url", ""),
                    "thumbnail": w.get("thumbnail", ""),
                }
                for w in works
            ]

            return json.dumps({
                "found": True,
                "message": f"为你找到了 {len(videos)} 个热门视频 🔥",
                "videos": videos,
            }, ensure_ascii=False)

    except Exception as e:
        log.error(f"获取热门视频失败: {e}")
        return json.dumps({"found": False, "message": "热门视频暂时不可用", "videos": []}, ensure_ascii=False)


# ===================== 内部辅助函数 =====================

async def _extract_tags(user_query: str) -> List[str]:
    """用 DeepSeek 从用户查询中提取标签"""
    prompt = f"""你是一个视频标签提取器。用户想要搜索视频，请从标签列表中选择最匹配的标签，返回JSON数组。

标签列表:
{all_tags_for_prompt()}

规则:
- 用户可能说"推荐XX视频"、"给我XX的"、"有没有XX"等
- 从标签列表中选择所有符合的标签(大类+子类都要选)
- 如果用户明确说了属性(如"黑丝")，一定要选上
- 如果用户没提的属性不要选
- 只返回JSON数组,如: ["美女","黑丝","长腿"]
- 如果用户没提任何具体类型，根据常见理解选择最合理的分类

用户查询: {user_query}

JSON数组:"""

    try:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.post(
                f"{settings.DEEPSEEK_API_URL}/chat/completions",
                headers={
                    "Authorization": f"Bearer {settings.DEEPSEEK_API_KEY}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": settings.DEEPSEEK_MODEL,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": 0.1,
                },
            )

            if resp.status_code != 200:
                log.error(f"标签提取 API 错误: {resp.status_code}")
                return []

            data = resp.json()
            content = data["choices"][0]["message"]["content"].strip()

            # 清理 markdown 代码块
            if content.startswith("```"):
                s = content.find("[")
                e = content.rfind("]")
                if s >= 0 and e > s:
                    content = content[s:e + 1]

            tags = json.loads(content)
            return tags if isinstance(tags, list) else []

    except Exception as e:
        log.warning(f"标签提取失败: {e}")
        return []
