package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.entity.User;
import com.example.dyhouduan.service.UserService;
import com.example.dyhouduan.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/{id}")
    public Response<User> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getById(id);
            if (user == null) {
                return Response.error(404, "用户不存在");
            }
            return Response.success(user);
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return Response.error("获取用户信息失败: " + e.getMessage());
        }
    }

    @PutMapping("/background")
    public Response<User> updateBackground(@RequestBody Map<String, String> requestBody, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);
            if (userId == null) {
                return Response.error(401, "未登录");
            }
            String background = requestBody.get("background");
            User user = userService.updateBackground(userId, background);
            log.info("用户[{}]更新背景颜色为: {}", userId, background);
            return Response.success(user);
        } catch (Exception e) {
            log.error("更新背景失败", e);
            return Response.error("更新背景失败: " + e.getMessage());
        }
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