# AI 助手流式输出改造方案（SSE）

## Context

当前 AI 对话链路全程非流式：前端 axios 一次性等完整 JSON（10s 超时）→ Java HttpClient 等 Python 完整响应（60s）→ Python `agent_graph.ainvoke()` 等全部生成完。用户要等 5-10 秒才能看到任何内容，体验差。

目标：改为 SSE 流式输出，首字延迟降到 ~1s；AI 调用工具搜索期间显示"正在为你找视频..."状态提示（用户已确认需要）。视频卡片获取本次保持串行，不并行化（用户选择 Other，保持改动范围最小）。

## SSE 协议设计

标准 SSE 格式 `event: <type>\ndata: <json>\n\n`，共 6 种事件（Python 定义，Java 透传，前端消费）：

| 事件 | data 内容 | 时机 |
|------|----------|------|
| `meta` | `{"thread_id": "..."}` | 连接建立后立即 |
| `status` | `{"stage": "searching", "tool": "recommend_videos"}` | on_tool_start 工具开始 |
| `token` | `{"text": "增量文本"}` | LLM 每个 token |
| `videos` | `{"videos": [...]}` | [VIDEOS] 解析完、卡片取回后 |
| `done` | `{"text": "完整文本", "type": "chat\|recommend"}` | 正常结束 |
| `error` | `{"message": "..."}` | 异常 |

## 改动清单

### 1. Python agent-service/main.py（核心）

新增 `POST /chat/stream` 端点（**保留现有 `/chat` 不动**，Java 降级仍用它）：

- 返回 `StreamingResponse(async_generator, media_type="text/event-stream")`，headers 加 `Cache-Control: no-cache`、`X-Accel-Buffering: no`
- 用 `agent_graph.astream_events({"messages": messages}, config, version="v2")` 事件流：
  - `on_chat_model_stream` → `event.data["chunk"].content` 非空字符串时发 `token` 事件（tool_call 生成阶段 content 为空，天然过滤）
  - `on_tool_start` → 发 `status` 事件（带 event.name）
- **[VIDEOS] 标记拦截**（关键逻辑，写在新的辅助函数 `_stream_safe_text`）：
  - 维护累计 buffer，每个 token 追加后只放出"安全前缀"：若末尾匹配 `[VIDEOS]` 的部分前缀（最多扣留 7 字符）则暂不发出
  - 一旦正则匹配到完整 `\[VIDEOS\]...[/VIDEOS]`：停止吐 token，用现有的 `_extract_video_ids()`（[main.py#L166](file:///c:/Users/27036/Desktop/dydemo/agent-service/main.py#L166)）提取 ID
  - 结束后调用现有 `_fetch_video_cards()`（[main.py#L182](file:///c:/Users/27036/Desktop/dydemo/agent-service/main.py#L182)，保持串行）→ 发 `videos` → 发 `done`（带已发出的干净全文）
- 异常路径发 `error` 事件而非抛异常
- 复用现有：`_extract_video_ids`、`_fetch_video_cards`；`_strip_video_tag` 的正则可复用

### 2. Java dyhouduan — ChatController.java

新增 `POST /api/chat/agent/stream`，返回 `SseEmitter`（timeout 120_000）：

- 复用类中现有 `httpClient`（[ChatController.java#L45](file:///c:/Users/27036/Desktop/dydemo/dyhouduan/src/main/java/com/example/dyhouduan/controller/ChatController.java#L45)）请求 Python `/chat/stream`，body 同现有 `/agent` 端点，`HttpResponse.BodyHandlers.ofInputStream()`
- 新增一个小的固定线程池字段（如 `Executors.newFixedThreadPool(8)`），把"读上游 SSE → 转发 SseEmitter"提交到线程池异步执行（不占 Tomcat 请求线程）
- BufferedReader 逐行解析上游 SSE（`event:` 行 / `data:` 行 / 空行分隔），用现有 `objectMapper` 校验 JSON 后 `emitter.send(SseEmitter.event().name(evt).data(json))` 透传
- 正常读完 `emitter.complete()`；异常 `emitter.completeWithError()`
- **现有 `/api/chat/agent` 非流式端点不动**（降级链路 `handleWithFallback` 继续有效）

### 3. 前端 dyqianduan — MainLayout.vue

`sendChatMessage()` 的 AI_BOT 分支（[MainLayout.vue#L511-L557](file:///c:/Users/27036/Desktop/dydemo/dyqianduan/src/components/MainLayout.vue#L511-L557)）：

- **弃用 axios 改用原生 `fetch('/api/chat/agent/stream', ...)`**：axios 有 10s timeout 会掐断 SSE；fetch 手动带 `Authorization: Bearer ${localStorage.getItem('token')}`（request.js 的拦截器对 fetch 不生效）
- `response.body.getReader()` + `TextDecoder` 循环读块，按行解析 SSE（注意跨块断行需缓存残行）
- 事件处理：
  - `meta` → 更新 `threadId.value`
  - `status` → 立即 push 一条 streaming 中的 botMsg，显示"正在为你找视频..."（status 阶段）
  - `token` → 追加 `botMsg.content`，`nextTick` 后 `scrollToBottom()`
  - `videos` → 设置 `botMsg.isRecommend / recommendVideos`
  - `done` → 清除状态标记，`saveBotMessage(完整文本)`
  - `error` → botMsg 显示兜底文案
- botMsg 加 `streaming` 临时字段驱动状态显示
- 模板微调（[MainLayout.vue#L193](file:///c:/Users/27036/Desktop/dydemo/dyqianduan/src/components/MainLayout.vue#L193)）：`msg.content.text` 改为 `msg.content?.text || msg.content`，兼容流式期间的字符串 content（顺带修复现有 recommend 消息文本不显示的小 bug）；新增 `v-if="msg.streaming"` 的状态/打字指示

## 验证

1. **Python 层**：`curl -N -X POST http://localhost:8000/chat/stream -H "Content-Type: application/json" -d "{\"messages\":[{\"role\":\"user\",\"content\":\"推荐一个视频\"}]}"` — 应逐个看到 meta/status/token，最后 videos + done；确认 `[VIDEOS]` 标记不出现在任何 token 中
2. **Java 层**：`curl -N -X POST http://localhost:8080/api/chat/agent/stream ...` — 事件原样透传
3. **前端**：`npm run dev` 后实测两场景：闲聊（无工具、纯 token 流）和"推荐视频"（状态提示 → 吐字 → 卡片出现）；再测"换一批"排除逻辑
4. **异常**：停掉 Python 服务再发消息，前端应显示兜底文案而非卡死

## 风险点

- DeepSeek 原生支持 stream，ChatOpenAI 走 astream_events 无兼容问题
- vite dev proxy 默认支持 SSE 透传；若生产用 nginx 需加 `proxy_buffering off`（本地开发暂不涉及）
- `[VIDEOS]` 标记跨 token 拦截是主要测试点（标记可能被拆成 `[`、`VIDEO`、`S]` 多个 token）
- SseEmitter 转发线程池要限制大小，避免并发聊天耗尽线程
