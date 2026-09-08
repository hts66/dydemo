package com.example.dyhouduan.config;

import com.example.dyhouduan.websocket.ChatWebSocketHandler;
import com.example.dyhouduan.websocket.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置：注册实时聊天端点
 * 端点路径放在 /api 下，方便前端 vite 代理统一转发
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/api/ws/chat")
                .addInterceptors(authInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
