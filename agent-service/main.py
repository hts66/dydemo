"""FastAPI 入口 — Agent 对话服务"""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from config import settings
from models import ChatRequest, ChatResponse, HealthResponse
from agent.graph import agent_graph

# 日志
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
log = logging.getLogger("agent-service")


# ===================== 生命周期 =====================

@asynccontextmanager
async def lifespan(app: FastAPI):
    """启动时初始化 Qdrant collection"""
    from services.qdrant_client import qdrant_client

    log.info("🤖 Agent 服务启动中...")
    try:
        await qdrant_client.ensure_collection()
        log.info("✅ Qdrant collection 就绪")
    except Exception as e:
        log.warning(f"⚠️ Qdrant 初始化失败（可能未启动）: {e}")

    log.info(f"🚀 Agent 服务已就绪: http://0.0.0.0:{settings.AGENT_PORT}")
    yield
    log.info("Agent 服务关闭")


# ===================== App =====================

app = FastAPI(
    title="短视频平台 AI Agent",
    description="基于 LangGraph + DeepSeek + Qdrant 的智能视频推荐 Agent",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ===================== 端点 =====================

@app.get("/health", response_model=HealthResponse)
async def health():
    return HealthResponse()


@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """Agent 对话接口 — 接收多轮对话，返回智能回复 + 可选视频推荐"""
    try:
        # 将 ChatMessage 列表转为 LangChain 格式
        messages = [{"role": m.role, "content": m.content} for m in request.messages]

        if not messages:
            raise HTTPException(status_code=400, detail="消息列表为空")

        log.info(f"收到对话请求: {messages[-1]['content'][:50]}...")

        # 调用 LangGraph Agent
        result = await agent_graph.ainvoke({"messages": messages})

        # 提取最后一条 AI 消息
        ai_messages = [m for m in result["messages"] if m.type == "ai"]
        if not ai_messages:
            return ChatResponse(type="chat", text="抱歉，我暂时无法回答你的问题。")

        last_ai = ai_messages[-1]
        reply_text = last_ai.content if hasattr(last_ai, "content") else str(last_ai)

        # 解析 [VIDEOS] 标记，提取视频ID
        video_ids = _extract_video_ids(reply_text)
        reply_text = _strip_video_tag(reply_text)

        # 如果有视频ID，调用 Spring Boot 获取完整视频信息
        videos = []
        if video_ids:
            videos = await _fetch_video_cards(video_ids)

        response_type = "recommend" if videos else "chat"

        return ChatResponse(
            type=response_type,
            text=reply_text.strip(),
            videos=videos,
        )

    except HTTPException:
        raise
    except Exception as e:
        log.error(f"Agent 处理失败: {e}", exc_info=True)
        return ChatResponse(type="chat", text="抱歉，我暂时无法回答你的问题。")


# ===================== 辅助函数 =====================

def _extract_video_ids(text: str) -> list:
    """从回复中提取 [VIDEOS] 标记的视频ID"""
    import re
    match = re.search(r'\[VIDEOS\]\s*([\d,\s]+)\s*\[/VIDEOS\]', text)
    if match:
        ids_str = match.group(1)
        return [int(x.strip()) for x in ids_str.split(",") if x.strip().isdigit()]
    return []


def _strip_video_tag(text: str) -> str:
    """移除回复中的 [VIDEOS] 标记"""
    import re
    return re.sub(r'\[VIDEOS\].*?\[/VIDEOS\]', '', text, flags=re.DOTALL).strip()


async def _fetch_video_cards(video_ids: list) -> list:
    """调用 Spring Boot API 获取视频详情"""
    import httpx

    videos = []
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            for vid in video_ids[:5]:  # 最多5个
                resp = await client.get(f"{settings.SPRING_BOOT_API_URL}/api/works/{vid}")
                if resp.status_code == 200:
                    data = resp.json()
                    if data.get("code") == 200:
                        w = data["data"]
                        videos.append({
                            "id": w["id"],
                            "title": w.get("title", ""),
                            "description": w.get("description", ""),
                            "url": w.get("url", ""),
                            "thumbnail": w.get("thumbnail", ""),
                            "username": w.get("username", "匿名用户"),
                            "score": 0.0,
                        })
    except Exception as e:
        log.warning(f"获取视频详情失败: {e}")

    return videos
