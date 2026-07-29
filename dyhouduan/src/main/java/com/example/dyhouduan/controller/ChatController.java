package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.service.DeepSeekService;
import com.example.dyhouduan.service.VideoRecommendService;
import com.example.dyhouduan.service.VideoRecommendService.RecommendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final DeepSeekService deepSeekService;
    private final VideoRecommendService videoRecommendService;

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
