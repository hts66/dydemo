"""LangChain 工具定义 — Agent 可调用的能力"""

import json
import logging
import random
from typing import List, Optional
import httpx

from langchain_core.tools import tool

from config import settings
from services.tag_vocabulary import build_vector, all_tags_for_prompt, TAG_MAP
from services.qdrant_client import qdrant_client
from services.embedding_client import embed

log = logging.getLogger(__name__)


@tool
async def recommend_videos(query: str, limit: int = 5, exclude_ids: Optional[str] = None) -> str:
    """
    根据用户的自然语言描述推荐视频。当用户说"推荐XX视频"、"给我找XX"、"有没有XX"等时调用此工具。
    内部会先用AI提取标签，再通过向量搜索匹配最相似的视频。向量搜索无结果时自动降级为关键词搜索。

    **重要 — 避免重复推荐**:
    - 用户说"换一批"、"再来几个"、"还有吗"时，必须把之前推荐过的所有视频ID传入 exclude_ids
    - exclude_ids 格式为逗号分隔的ID字符串，如 "3,7,12"
    - 首次推荐不传 exclude_ids

    精确度优先：只返回相似度 score >= 0.3 的视频，宁少勿滥。

    Args:
        query: 用户的原始查询语句，例如"推荐御姐黑丝长腿视频"
        limit: 最多返回视频数量，默认5个
        exclude_ids: 逗号分隔的已推荐视频ID，如 "1,3,5"。传空字符串表示不排除

    Returns:
        JSON格式的推荐结果，包含视频列表
    """
    try:
        # 解析排除列表
        excluded = set()
        if exclude_ids and exclude_ids.strip():
            try:
                excluded = {int(x.strip()) for x in exclude_ids.split(",") if x.strip().isdigit()}
            except Exception:
                pass
        log.info(f"推荐请求: '{query}' limit={limit} exclude={excluded}")

        # 1. 对查询做语义 embedding
        vector = await embed(query)
        if not vector:
            return json.dumps({
                "found": False,
                "message": f"没有找到与「{query}」相关的视频，换个关键词试试吧~",
                "videos": []
            }, ensure_ascii=False)

        # 2. Qdrant 语义向量检索（多取一些，供排除已看 + 随机采样）
        vector_results = await qdrant_client.search_similar(vector, max(limit * 4, 20))
        log.info(f"语义检索: '{query}' → {len(vector_results)} 条")

        if not vector_results:
            return json.dumps({
                "found": False,
                "message": f"没有找到与「{query}」相关的视频，换个关键词试试吧~",
                "videos": []
            }, ensure_ascii=False)

        # 3. 格式化 → 排除已推荐 → 随机采样 → 不排序
        all_videos = []
        for r in vector_results:
            payload = r.get("payload", {})
            all_videos.append({
                "id": r["id"],
                "title": payload.get("title", ""),
                "description": payload.get("description", ""),
                "tags": payload.get("tags", []),
            })

        available = [v for v in all_videos if v["id"] not in excluded]

        if not available:
            return json.dumps({
                "found": False,
                "message": f"「{query}」相关的视频看完啦，试试其他类型吧~ 😊",
                "videos": [],
            }, ensure_ascii=False)

        if len(available) > limit:
            videos = random.sample(available, limit)
        else:
            videos = available

        if excluded:
            message = f"为你换了一批「{query}」相关的视频，共 {len(videos)} 个，快来看看吧~ 🎬"
        else:
            message = f"为你找到了 {len(videos)} 个「{query}」相关的视频，快来看看吧~ 🎬"

        return json.dumps({
            "found": True,
            "message": message,
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
    """用 DeepSeek 从用户查询中提取标签（只保留标签表中存在的）"""
    prompt = f"""你是一个视频标签提取器。从下方标签列表中选择用户想要的标签，返回JSON数组。

## 标签列表
{all_tags_for_prompt()}

## 规则
- 第一项必须是大类(美女/小猫/风景)
- 用户明确提到的属性才选，没提到的不要猜
- 标签列表中找不到对应标签 → 返回[]
- 示例:
  "推荐黑丝视频" → ["美女","黑丝"]
  "我要看JK制服" → ["美女","JK制服"]
  "有没有跳舞的美女" → ["美女","单人舞"]
  "推荐橘猫" → ["小猫","橘猫"]
  "赛车视频" → []

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
            if isinstance(tags, list):
                # 只保留标签表中存在的词，过滤 LLM 编造的
                valid = [t for t in tags if t in TAG_MAP]
                if len(valid) != len(tags):
                    log.info(f"标签过滤: {tags} → {valid}")
                return valid
            return []

    except Exception as e:
        log.warning(f"标签提取失败: {e}")
        return []


async def _keyword_search(keyword: str, limit: int = 5) -> list:
    """关键词搜索（MySQL LIKE），作为向量搜索的降级方案"""
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            resp = await client.get(
                f"{settings.SPRING_BOOT_API_URL}/api/works",
                params={"keyword": keyword, "page": 1, "size": limit},
            )
            if resp.status_code != 200:
                return []

            data = resp.json()
            if data.get("code") != 200:
                return []

            raw = data["data"]
            works = raw if isinstance(raw, list) else raw.get("records", [])
            return [
                {
                    "id": w["id"],
                    "score": 0.0,
                    "title": w.get("title", ""),
                    "description": w.get("description", ""),
                    "tags": [],
                }
                for w in works
            ]
    except Exception as e:
        log.error(f"关键词搜索降级失败: {e}")
        return []
