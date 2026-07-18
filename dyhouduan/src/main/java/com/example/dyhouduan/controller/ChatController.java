package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.service.DeepSeekService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final DeepSeekService deepSeekService;

    public ChatController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/bot")
    public Response<String> chatWithBot(@RequestBody Map<String, Object> request) {
        // 新格式 (多轮对话): {"messages": [{"role":"user","content":"..."}, ...]}
        if (request.containsKey("messages")) {
            List<Map<String, Object>> raw = (List<Map<String, Object>>) request.get("messages");
            if (raw == null || raw.isEmpty()) {
                return Response.error("消息内容不能为空");
            }
            List<Map<String, String>> messages = new ArrayList<>();
            for (Map<String, Object> m : raw) {
                Map<String, String> map = new HashMap<>();
                map.put("role", (String) m.get("role"));
                map.put("content", (String) m.get("content"));
                messages.add(map);
            }
            String last = messages.get(messages.size() - 1).get("content");
            log.info("AI对话(多轮): {}", last);
            String reply = deepSeekService.chatWithHistory(messages);
            return Response.success(reply);
        }

        // 旧格式 (单轮): {"message": "..."}
        String message = (String) request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return Response.error("消息内容不能为空");
        }
        log.info("AI对话(单轮): {}", message);
        String reply = deepSeekService.chat(message);
        return Response.success(reply);
    }
}
