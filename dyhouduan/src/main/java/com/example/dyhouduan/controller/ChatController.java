package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.service.DeepSeekService;
import com.example.dyhouduan.service.VideoRecommendService;
import com.example.dyhouduan.service.VideoRecommendService.RecommendResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final DeepSeekService deepSeekService;
    private final VideoRecommendService videoRecommendService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ExecutorService sseExecutor;

    @Value("${agent.api-url:http://localhost:8000}")
    private String agentApiUrl;

    // 推荐意图关键词
    private static final String[] RECOMMEND_KEYWORDS = {
        "推荐", "找个", "来个", "找一个", "来一个", "有没有", "帮我找",
        "想看", "给我", "我要看", "播放", "放一个", "来点"
    };
    private static final String[] VIDEO_KEYWORDS = {
        "视频", "作品", "内容"
    };

    public ChatController(DeepSeekService deepSeekService, VideoRecommendService videoRecommendService) {
        this.deepSeekService = deepSeekService;
        this.videoRecommendService = videoRecommendService;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();
        this.sseExecutor = Executors.newFixedThreadPool(8);
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/bot")
    public Response<Object> chatWithBot(@RequestBody Map<String, Object> request) {
        String message;

        // 新格式 (多轮对话)
        if (request.containsKey("messages")) {
            List<Map<String, Object>> raw = (List<Map<String, Object>>) request.get("messages");
            if (raw == null || raw.isEmpty()) {
                return Response.error("消息内容不能为空");
            }
            // 取最后一条用户消息进行意图检测
            String lastUserMsg = "";
            List<Map<String, String>> messages = new ArrayList<>();
            for (Map<String, Object> m : raw) {
                Map<String, String> map = new HashMap<>();
                map.put("role", (String) m.get("role"));
                map.put("content", (String) m.get("content"));
                messages.add(map);
                if ("user".equals(m.get("role"))) {
                    lastUserMsg = (String) m.get("content");
                }
            }
            message = lastUserMsg;

            // 检测推荐意图
            if (isRecommendIntent(message)) {
                log.info("检测到推荐意图: {}", message);
                RecommendResult result = videoRecommendService.recommend(message, 5);
                return buildRecommendResponse(result);
            }

            log.info("AI对话(多轮): {}", message);
            String reply = deepSeekService.chatWithHistory(messages);
            return Response.success(reply);

        } else {
            // 旧格式 (单轮)
            message = (String) request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return Response.error("消息内容不能为空");
            }

            if (isRecommendIntent(message)) {
                log.info("检测到推荐意图: {}", message);
                RecommendResult result = videoRecommendService.recommend(message, 5);
                return buildRecommendResponse(result);
            }

            log.info("AI对话(单轮): {}", message);
            String reply = deepSeekService.chat(message);
            return Response.success(reply);
        }
    }

    /** ===================== Agent 端点 ===================== */
    @SuppressWarnings("unchecked")
    @PostMapping("/agent")
    public Response<Object> chatWithAgent(@RequestBody Map<String, Object> request) {
        try {
            // 转发请求到 FastAPI Agent 服务
            String agentUrl = agentApiUrl + "/chat";
            String jsonBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(agentUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 直接透传 Agent 返回的 JSON
                var agentResult = objectMapper.readTree(response.body());
                String type = agentResult.has("type") ? agentResult.get("type").asText() : "chat";
                String text = agentResult.has("text") ? agentResult.get("text").asText() : "";
                String threadId = agentResult.has("thread_id") ? agentResult.get("thread_id").asText() : "";

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("type", type);
                data.put("text", text);
                if (!threadId.isEmpty()) {
                    data.put("thread_id", threadId);
                }

                if (agentResult.has("videos") && !agentResult.get("videos").isEmpty()) {
                    data.put("videos", objectMapper.convertValue(
                            agentResult.get("videos"), List.class));
                }

                return Response.success(data);
            } else {
                log.warn("Agent 服务返回异常: {} {}", response.statusCode(), response.body());
                // 降级到旧逻辑
                return handleWithFallback(request);
            }
        } catch (Exception e) {
            log.error("Agent 服务调用失败，降级到旧逻辑: {}", e.getMessage());
            // 降级到旧逻辑
            return handleWithFallback(request);
        }
    }

    /** ===================== Agent 流式端点（SSE 透传） ===================== */
    @PostMapping("/agent/stream")
    public SseEmitter chatWithAgentStream(@RequestBody Map<String, Object> request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        sseExecutor.execute(() -> {
            try {
                String agentUrl = agentApiUrl + "/chat/stream";
                String jsonBody = objectMapper.writeValueAsString(request);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(agentUrl))
                        .header("Content-Type", "application/json")
                        .header("Accept", "text/event-stream")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(110))
                        .build();

                HttpResponse<java.io.InputStream> response = httpClient.send(httpRequest,
                        HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    log.warn("Agent 流式服务返回异常: {}", response.statusCode());
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"message\":\"抱歉，我暂时无法回答你的问题。\"}"));
                    emitter.complete();
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    String line;
                    String eventName = null;
                    StringBuilder dataBuf = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("event:")) {
                            eventName = line.substring(6).trim();
                        } else if (line.startsWith("data:")) {
                            dataBuf.append(line.substring(5).trim());
                        } else if (line.isEmpty()) {
                            // 空行 = 事件边界，透传给前端
                            if (eventName != null && dataBuf.length() > 0) {
                                objectMapper.readTree(dataBuf.toString()); // 校验 JSON 合法
                                emitter.send(SseEmitter.event().name(eventName).data(dataBuf.toString()));
                            }
                            eventName = null;
                            dataBuf.setLength(0);
                        }
                    }
                    // 流意外结束时冲刷残留事件
                    if (eventName != null && dataBuf.length() > 0) {
                        objectMapper.readTree(dataBuf.toString());
                        emitter.send(SseEmitter.event().name(eventName).data(dataBuf.toString()));
                    }
                }
                emitter.complete();
            } catch (Exception e) {
                log.error("Agent 流式转发失败: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"message\":\"抱歉，我暂时无法回答你的问题。\"}"));
                    emitter.complete();
                } catch (Exception ignore) {
                    emitter.completeWithError(e);
                }
            }
        });
        return emitter;
    }

    /** ===================== 重置会话端点 ===================== */
    @PostMapping("/agent/reset")
    public Response<Object> resetAgentChat(@RequestBody Map<String, Object> request) {
        try {
            String agentUrl = agentApiUrl + "/chat/reset";
            String jsonBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(agentUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var result = objectMapper.readTree(response.body());
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("new_thread_id", result.get("new_thread_id").asText());
                return Response.success(data);
            } else {
                return Response.error("重置会话失败");
            }
        } catch (Exception e) {
            log.error("重置Agent会话失败: {}", e.getMessage());
            return Response.error("重置会话失败: " + e.getMessage());
        }
    }

    /** 降级处理 — 使用旧的推荐 + 对话逻辑 */
    @SuppressWarnings("unchecked")
    private Response<Object> handleWithFallback(Map<String, Object> request) {
        log.info("使用降级逻辑处理请求");
        String message;

        if (request.containsKey("messages")) {
            List<Map<String, Object>> raw = (List<Map<String, Object>>) request.get("messages");
            if (raw == null || raw.isEmpty()) {
                return Response.error("消息内容不能为空");
            }
            String lastUserMsg = "";
            List<Map<String, String>> messages = new ArrayList<>();
            for (Map<String, Object> m : raw) {
                Map<String, String> map = new HashMap<>();
                map.put("role", (String) m.get("role"));
                map.put("content", (String) m.get("content"));
                messages.add(map);
                if ("user".equals(m.get("role"))) {
                    lastUserMsg = (String) m.get("content");
                }
            }
            message = lastUserMsg;

            if (isRecommendIntent(message)) {
                RecommendResult result = videoRecommendService.recommend(message, 5);
                return buildRecommendResponse(result);
            }

            String reply = deepSeekService.chatWithHistory(messages);
            return Response.success(reply);
        } else {
            message = (String) request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return Response.error("消息内容不能为空");
            }

            if (isRecommendIntent(message)) {
                RecommendResult result = videoRecommendService.recommend(message, 5);
                return buildRecommendResponse(result);
            }

            String reply = deepSeekService.chat(message);
            return Response.success(reply);
        }
    }

    /** 检测是否为视频推荐意图 */
    private boolean isRecommendIntent(String msg) {
        if (msg == null) return false;
        String lower = msg.toLowerCase().trim();

        boolean hasRecKeyword = false;
        for (String kw : RECOMMEND_KEYWORDS) {
            if (lower.contains(kw)) {
                hasRecKeyword = true;
                break;
            }
        }
        if (!hasRecKeyword) return false;

        boolean hasVideoKeyword = false;
        for (String kw : VIDEO_KEYWORDS) {
            if (lower.contains(kw)) {
                hasVideoKeyword = true;
                break;
            }
        }
        if (!hasVideoKeyword) return false;

        return true;
    }

    /** 构建推荐响应 */
    private Response<Object> buildRecommendResponse(RecommendResult result) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "recommend");
        data.put("text", result.replyText());
        data.put("videos", result.videos());
        return Response.success(data);
    }
}
