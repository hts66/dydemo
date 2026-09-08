package com.example.dyhouduan.websocket;

import com.example.dyhouduan.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * WebSocket 握手鉴权拦截器：从 URL 参数中取 JWT 验证，通过后把 userId 绑定到连接
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build().getQueryParams().getFirst("token");
        if (token == null || token.isBlank()) {
            log.warn("WebSocket握手缺少token: {}", request.getURI());
            return false;
        }
        try {
            Long userId = jwtUtil.getUserId(token);
            attributes.put("userId", userId);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket握手鉴权失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }
}
