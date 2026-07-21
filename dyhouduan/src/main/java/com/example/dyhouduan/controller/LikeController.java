package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.entity.Work;
import com.example.dyhouduan.service.LikeService;
import com.example.dyhouduan.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            log.info("用户[{}]对作品[{}]执行点赞操作，结果：{}", userId, workId, isLiked ? "已点赞" : "已取消点赞");
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
            log.info("用户[{}]查询作品[{}]的点赞状态：{}", userId, workId, isLiked);
            return Response.success(isLiked);
        } catch (Exception e) {
            log.error("查询点赞状态失败", e);
            return Response.error("查询点赞状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/list/{userId}")
    public Response<List<Work>> getLikedWorks(@PathVariable Long userId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "12") int size) {
        try {
            List<Work> works = likeService.getLikedWorks(userId, page, size);
            return Response.success(works);
        } catch (Exception e) {
            log.error("获取点赞列表失败", e);
            return Response.error("获取点赞列表失败: " + e.getMessage());
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
