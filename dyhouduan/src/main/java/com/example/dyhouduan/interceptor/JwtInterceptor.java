package com.example.dyhouduan.interceptor;

import com.example.dyhouduan.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // Public read endpoints also pass through this interceptor. Requests without a
        // token may continue; controllers that require a login still return 401.
        if (token == null || token.isEmpty()) {
            return true;
        }

        try {
            if (jwtUtil.isTokenExpired(token)) {
                writeUnauthorized(response, "登录状态已过期，请重新登录");
                return false;
            }

            if (!jwtUtil.isAccessToken(token)) {
                writeUnauthorized(response, "无效的访问令牌");
                return false;
            }

            Long userId = jwtUtil.getUserId(token);
            String email = jwtUtil.getEmail(token);

            request.setAttribute("userId", userId);
            request.setAttribute("email", email);
        } catch (Exception e) {
            writeUnauthorized(response, "登录状态无效，请重新登录");
            return false;
        }

        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
    }
}
