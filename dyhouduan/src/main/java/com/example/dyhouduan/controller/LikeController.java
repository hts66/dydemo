package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.service.LikeService;
import com.example.dyhouduan.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/likes")
public class LikeController {

    @Autowired
    private LikeService likeService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/{workId}")
    public Response<Boolean> toggleLike(@PathVariable Long workId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);
            if (userId == null) {
                return Response.error(401, "未登录");
            }
            boolean isLiked = likeService.toggleLike(userId, workId);
            return Response.success(isLiked);
        } catch (Exception e) {
            log.error("点赞失败", e);
            return Response.error("点赞失败: " + e.getMessage());
        }
    }

    @GetMapping("/{workId}")
    public Response<Boolean> isLiked(@PathVariable Long workId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);
            if (userId == null) {
                return Response.success(false);
            }
            boolean isLiked = likeService.isLiked(userId, workId);
            return Response.success(isLiked);
        } catch (Exception e) {
            log.error("查询点赞状态失败", e);
            return Response.error("查询点赞状态失败: " + e.getMessage());
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
