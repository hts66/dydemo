package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.service.DeepSeekService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final DeepSeekService deepSeekService;

    public ChatController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @PostMapping("/bot")
    public Response<String> chatWithBot(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return Response.error("消息内容不能为空");
        }
        
        log.info("用户@机器人消息: {}", message);
        
        String reply = deepSeekService.chat(message);
        return Response.success(reply);
    }
}
