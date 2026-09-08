"""FastAPI 入口 — Agent 对话服务"""

import json
import logging
import re
import uuid
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, StreamingResponse

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


# ===================== 流式对话（SSE） =====================

VIDEO_MARKER_RE = re.compile(r'\[VIDEOS\]\s*([\d,\s]+)\s*\[/VIDEOS\]')
VIDEO_MARKER_START = "[VIDEOS]"


def _sse(event: str, data: dict) -> str:
    """格式化一条 SSE 事件"""
    return f"event: {event}\ndata: {json.dumps(data, ensure_ascii=False)}\n\n"


class _VideoMarkerFilter:
    """流式输出中拦截 [VIDEOS]...[/VIDEOS] 标记，防止标记泄漏给前端

    原理：累计原始文本 raw，只放出"安全前缀"：
    - 末尾若匹配 [VIDEOS] 的部分前缀（最多扣留 7 字符）则暂不发出
    - 检测到完整起始标记后停止吐字，直到 [/VIDEOS] 闭合后提取视频ID
    """

    def __init__(self):
        self.raw = ""
        self.sent = 0            # raw 中已安全发出的字符数
        self.marker_open = False  # 已见起始标记，等待闭合
        self.done = False         # 完整标记已解析
        self.video_ids = []

    def feed(self, token: str) -> str:
        """追加 token，返回本次可安全发出的文本"""
        if self.done:
            return ""
        self.raw += token
        text = ""
        if not self.marker_open:
            idx = self.raw.find(VIDEO_MARKER_START, self.sent)
            if idx >= 0:
                text = self.raw[self.sent:idx]
                self.sent = idx
                self.marker_open = True
            else:
                # 无完整起始标记 → 扣留末尾可能是标记前缀的部分
                end = len(self.raw)
                hold = min(len(VIDEO_MARKER_START) - 1, len(self.raw) - self.sent)
                for k in range(hold, 0, -1):
                    if self.raw.endswith(VIDEO_MARKER_START[:k]):
                        end = len(self.raw) - k
                        break
                text = self.raw[self.sent:end]
                self.sent = end
                return text
        # marker_open：检查是否已闭合
        m = VIDEO_MARKER_RE.search(self.raw, self.sent)
        if m:
            self.done = True
            self.video_ids = [int(x.strip()) for x in m.group(1).split(",") if x.strip().isdigit()]
        return text

    def final_text(self) -> str:
        """流结束后的干净文本（与已发出的 token 保持一致）"""
        return self.raw[:self.sent].strip()


@app.post("/chat/stream")
async def chat_stream(request: ChatRequest):
    """Agent 流式对话接口 — SSE 输出，事件: meta/status/token/videos/done/error"""
    messages = [{"role": m.role, "content": m.content} for m in request.messages]
    if not messages:
        raise HTTPException(status_code=400, detail="消息列表为空")

    thread_id = request.thread_id or str(uuid.uuid4())
    config = {"configurable": {"thread_id": thread_id}}

    async def event_gen():
        yield _sse("meta", {"thread_id": thread_id})
        filt = _VideoMarkerFilter()
        full_text = ""
        try:
            log.info(f"收到流式对话请求 [thread={thread_id[:8]}]: {messages[-1]['content'][:50]}...")
            async for event in agent_graph.astream_events(
                {"messages": messages}, config=config, version="v2",
            ):
                kind = event["event"]
                if kind == "on_tool_start":
                    yield _sse("status", {"stage": "searching", "tool": event.get("name", "")})
                elif kind == "on_chat_model_stream":
                    chunk = event["data"]["chunk"]
                    content = chunk.content if isinstance(chunk.content, str) else ""
                    if not content:
                        continue
                    safe = filt.feed(content)
                    if safe:
                        full_text += safe
                        yield _sse("token", {"text": safe})

            video_ids = filt.video_ids
            videos = await _fetch_video_cards(video_ids) if video_ids else []
            if videos:
                yield _sse("videos", {"videos": videos})
            yield _sse("done", {
                "text": full_text,
                "type": "recommend" if videos else "chat",
            })
        except Exception as e:
            log.error(f"流式对话处理失败: {e}", exc_info=True)
            yield _sse("error", {"message": "抱歉，我暂时无法回答你的问题。"})

    return StreamingResponse(
        event_gen(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
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
