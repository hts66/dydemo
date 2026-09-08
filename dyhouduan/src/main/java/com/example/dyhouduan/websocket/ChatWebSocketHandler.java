package com.example.dyhouduan.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 实时聊天 WebSocket 处理器
 * 发送消息仍走 HTTP（复用落库逻辑），本连接只负责"接收推送"和心跳保活
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatSessionRegistry registry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            registry.register(userId, session);
            log.info("用户[{}]建立WebSocket连接: {}", userId, session.getId());
        } else {
            // 拦截器已拦截非法连接，这里双保险
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            registry.unregister(userId, session);
            log.info("用户[{}]断开WebSocket连接: {} ({})", userId, session.getId(), status);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 心跳保活：前端每30秒发 ping，回 pong
        if ("ping".equals(message.getPayload())) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (Exception ignore) {
            }
        }
    }
}
