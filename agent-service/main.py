"""FastAPI 入口 — Agent 对话服务"""

import logging
import uuid
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

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


# ===================== 异常处理（调试用） =====================

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """记录请求体验证失败的详细信息"""
    body = None
    try:
        body = await request.body()
        body_str = body.decode("utf-8")[:500]
    except Exception:
        body_str = "<无法读取>"
    log.error(f"❌ 请求验证失败 [{request.method} {request.url.path}]")
    log.error(f"   Body: {body_str}")
    log.error(f"   错误: {exc.errors()}")
    return JSONResponse(
        status_code=422,
        content={"detail": exc.errors(), "body": body_str},
    )


# ===================== 端点 =====================

@app.get("/health", response_model=HealthResponse)
async def health():
    return HealthResponse()


@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """Agent 对话接口 — 支持 thread_id 持续对话"""
    try:
        # 将 ChatMessage 列表转为 LangChain 格式
        messages = [{"role": m.role, "content": m.content} for m in request.messages]

        if not messages:
            raise HTTPException(status_code=400, detail="消息列表为空")

        # 会话管理：有 thread_id 则继续对话，否则创建新会话
        thread_id = request.thread_id or str(uuid.uuid4())
        config = {"configurable": {"thread_id": thread_id}}

        log.info(f"收到对话请求 [thread={thread_id[:8]}]: {messages[-1]['content'][:50]}...")

        # 调用 LangGraph Agent（带 checkpointer，自动恢复/保存会话状态）
        result = await agent_graph.ainvoke(
            {"messages": messages},
            config=config,
        )

        # 提取最后一条 AI 消息
        ai_messages = [m for m in result["messages"] if m.type == "ai"]
        if not ai_messages:
            return ChatResponse(
                type="chat",
                text="抱歉，我暂时无法回答你的问题。",
                thread_id=thread_id,
            )

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
            thread_id=thread_id,
        )

    except HTTPException:
        raise
    except Exception as e:
        log.error(f"Agent 处理失败: {e}", exc_info=True)
        return ChatResponse(
            type="chat",
            text="抱歉，我暂时无法回答你的问题。",
            thread_id=request.thread_id,
        )


# ===================== 删除会话（清空对话记忆） =====================

@app.post("/chat/reset")
async def reset_chat(thread_id: str):
    """重置指定会话，清空对话记忆"""
    try:
        # MemorySaver 在当前进程中无法直接删除，但我们可以返回新的 thread_id
        # 客户端切换到新 thread_id 即相当于重置
        new_thread_id = str(uuid.uuid4())
        log.info(f"会话重置: {thread_id[:8]} → {new_thread_id[:8]}")
        return {"status": "ok", "new_thread_id": new_thread_id}
    except Exception as e:
        log.error(f"重置会话失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


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
                            "title": w.get("title") or "",
                            "description": w.get("description") or "",
                            "url": w.get("url") or "",
                            "thumbnail": w.get("thumbnail") or "",
                            "username": w.get("username") or "匿名用户",
                            "score": 0.0,
                        })
    except Exception as e:
        log.warning(f"获取视频详情失败: {e}")

    return videos
