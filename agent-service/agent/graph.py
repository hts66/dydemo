"""LangGraph Agent 图定义 + 系统提示词"""

from typing import List
from langchain_openai import ChatOpenAI
from langgraph.prebuilt import create_react_agent

from config import settings
from agent.tools import recommend_videos, search_videos_by_keyword, get_hot_videos

# ===================== 系统提示词 =====================

SYSTEM_PROMPT = """你是短视频平台的AI助手"小助" 🎬，活泼可爱、热情友好。

## 你的能力
1. **推荐视频** — 用户想看什么类型的视频，调用 recommend_videos 工具帮他们找
2. **搜索视频** — 用户想搜特定关键词的视频，调用 search_videos_by_keyword 工具
3. **热门推荐** — 用户想看热门/流行视频，调用 get_hot_videos 工具
4. **闲聊** — 日常问候、介绍平台功能等，直接回复即可

## 重要规则
- 回复简洁自然，像朋友聊天，2-4句话即可
- 用户说"推荐XX视频"、"给我找XX"、"有没有XX"时必须调用 recommend_videos
- 用户说"热门"、"有什么好看的"时调用 get_hot_videos
- 搜索关键词时调用 search_videos_by_keyword
- 工具返回的 JSON 中包含 videos 列表和 message，请用 message 的内容回复用户
- 如果工具返回 found=false，告诉用户没找到并建议换个关键词
- 不要编造视频信息，只使用工具返回的真实数据

## 回复格式要求
当工具返回了视频列表时，你必须在回复的最后一行附上：
```
[VIDEOS]
<视频ID列表，逗号分隔，如: 1,3,5>
[/VIDEOS]
```
这样前端才能展示视频卡片。如果没有视频，不需要这个标记。
"""

# ===================== 工具集合 =====================

TOOLS = [recommend_videos, search_videos_by_keyword, get_hot_videos]

# ===================== LLM 实例 =====================

llm = ChatOpenAI(
    model=settings.DEEPSEEK_MODEL,
    api_key=settings.DEEPSEEK_API_KEY,
    base_url=settings.DEEPSEEK_API_URL,
    temperature=0.7,
    max_tokens=1024,
)

# ===================== Agent 图 =====================

agent_graph = create_react_agent(
    model=llm,
    tools=TOOLS,
    state_modifier=SYSTEM_PROMPT,
)
