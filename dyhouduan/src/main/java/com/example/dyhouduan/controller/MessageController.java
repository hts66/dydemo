package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.entity.Message;
import com.example.dyhouduan.service.MessageService;
import com.example.dyhouduan.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/chat/{otherUserId}")
    public Response<List<Message>> getChatMessages(@PathVariable Long otherUserId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);
            if (userId == null) {
                return Response.error(401, "未登录");
            }
            List<Message> messages = messageService.getChatMessages(userId, otherUserId);
            log.info("用户[{}]获取与用户[{}]的聊天记录，共{}条消息", userId, otherUserId, messages.size());
            return Response.success(messages);
        } catch (Exception e) {
            log.error("获取聊天记录失败", e);
            return Response.error("获取聊天记录失败: " + e.getMessage());
        }
    }

    @PostMapping
    public Response<Message> sendMessage(@RequestBody MessageRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            if (userId == null) {
                return Response.error(401, "未登录");
            }
            Message message = messageService.sendMessage(userId, request.getReceiverId(), request.getContent());
            log.info("用户[{}]向用户[{}]发送消息：{}", userId, request.getReceiverId(), request.getContent());
            return Response.success(message);
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return Response.error("发送消息失败: " + e.getMessage());
        }
    }

    @PostMapping("/bot")
    public Response<Message> saveBotMessage(@RequestBody MessageRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            if (userId == null) {
                return Response.error(401, "未登录");
            }
            Message message = messageService.saveBotMessage(userId, request.getContent());
            return Response.success(message);
        } catch (Exception e) {
            log.error("保存机器人消息失败", e);
            return Response.error("保存机器人消息失败: " + e.getMessage());
        }
    }

    public static class MessageRequest {
        private Long receiverId;
        private String content;

        public Long getReceiverId() { return receiverId; }
        public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    private Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtUtil.getUserId(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}