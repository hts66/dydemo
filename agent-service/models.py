"""Pydantic 请求 / 响应模型"""

from typing import List, Optional
from pydantic import BaseModel, Field


# ===================== 请求模型 =====================

class ChatMessage(BaseModel):
    role: str = Field(..., description="角色: user / assistant / system")
    content: str = Field(..., description="消息内容")


class ChatRequest(BaseModel):
    messages: List[ChatMessage] = Field(..., description="对话历史，最后一条为当前用户消息")
    user_id: Optional[int] = Field(None, description="当前登录用户ID（可选）")


# ===================== 响应模型 =====================

class VideoCard(BaseModel):
    id: int
    title: str
    description: str
    url: str
    thumbnail: str
    username: str
    score: float = 0.0


class ChatResponse(BaseModel):
    type: str = Field("chat", description="chat / recommend")
    text: str = Field(..., description="AI 文本回复")
    videos: List[VideoCard] = Field(default_factory=list, description="推荐视频列表")
    thinking: Optional[str] = Field(None, description="Agent 思考过程（调试用）")


# ===================== 健康检查 =====================

class HealthResponse(BaseModel):
    status: str = "ok"
    version: str = "1.0.0"
