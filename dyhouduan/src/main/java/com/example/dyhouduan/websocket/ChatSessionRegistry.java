package com.example.dyhouduan.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 连接注册表：维护 userId → 在线连接 的映射（在线名单）
 * 一个用户可能开多个标签页，所以一个 userId 对应多个 session
 */
@Slf4j
@Component
public class ChatSessionRegistry {

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(Long userId, WebSocketSession session) {
        Set<WebSocketSession> set = sessions.get(userId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                sessions.remove(userId, set);
            }
        }
    }

    public boolean isOnline(Long userId) {
        Set<WebSocketSession> set = sessions.get(userId);
        return set != null && !set.isEmpty();
    }

    /**
     * 向用户的所有在线连接推送文本消息
     * @return 是否至少成功推送一次
     */
    public boolean sendToUser(Long userId, String payload) {
        Set<WebSocketSession> set = sessions.get(userId);
        if (set == null || set.isEmpty()) {
            // 接收方不在线：消息已落库，对方下次打开聊天页拉取即可
            log.info("推送跳过：用户[{}]当前无在线连接（离线，走拉取兜底）", userId);
            return false;
        }
        boolean sent = false;
        for (WebSocketSession session : set) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(payload));
                    }
                    sent = true;
                }
            } catch (Exception e) {
                log.warn("推送消息给用户[{}]失败: {}", userId, e.getMessage());
            }
        }
        return sent;
    }
}
