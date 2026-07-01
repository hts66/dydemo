package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.service.FollowService;
import com.example.dyhouduan.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/follows")
public class FollowController {

    @Autowired
    private FollowService followService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/{followeeId}")
    public Response<Boolean> toggleFollow(@PathVariable Long followeeId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);
            if (userId == null) {
                return Response.error(401, "未登录");
            }
            boolean isFollowing = followService.toggleFollow(userId, followeeId);
            log.info("用户[{}]对用户[{}]执行关注操作，结果：{}", userId, followeeId, isFollowing ? "已关注" : "已取消关注");
            return Response.success(isFollowing);
        } catch (Exception e) {
            log.error("关注操作失败", e);
            return Response.error("关注操作失败: " + e.getMessage());
        }
    }

    @GetMapping("/{followeeId}")
    public Response<Boolean> isFollowing(@PathVariable Long followeeId, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);
            if (userId == null) {
                return Response.success(false);
            }
            boolean isFollowing = followService.isFollowing(userId, followeeId);
            log.info("用户[{}]查询用户[{}]的关注状态：{}", userId, followeeId, isFollowing);
            return Response.success(isFollowing);
        } catch (Exception e) {
            log.error("查询关注状态失败", e);
            return Response.error("查询关注状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/list/{userId}")
    public Response<Map<String, Object>> getFollowList(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> following = followService.getFollowingList(userId);
            List<Map<String, Object>> followers = followService.getFollowerList(userId);
            Map<String, Object> data = new HashMap<>();
            data.put("following", following);
            data.put("followers", followers);
            return Response.success(data);
        } catch (Exception e) {
            log.error("获取关注列表失败", e);
            return Response.error("获取关注列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/friends/{userId}")
    public Response<List<Map<String, Object>>> getFriends(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> friends = followService.getMutualFriends(userId);
            return Response.success(friends);
        } catch (Exception e) {
            log.error("获取朋友列表失败", e);
            return Response.error("获取朋友列表失败: " + e.getMessage());
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
