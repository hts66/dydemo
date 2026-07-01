package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.entity.Work;
import com.example.dyhouduan.service.FollowService;
import com.example.dyhouduan.service.WorkService;
import com.example.dyhouduan.service.WatchHistoryService;
import com.example.dyhouduan.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/works")
public class WorkController {

    @Autowired
    private WorkService workService;

    @Autowired
    private FollowService followService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WatchHistoryService watchHistoryService;

    /**
     * 获取视频列表（分页）
     */
    @GetMapping
    public Response<List<Work>> getWorks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<Work> works = workService.getWorksWithUser(page, size);
            return Response.success(works);
        } catch (Exception e) {
            log.error("获取视频列表失败", e);
            return Response.error("获取视频列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个视频详情
     */
    @GetMapping("/{id}")
    public Response<Work> getWorkById(@PathVariable Long id) {
        try {
            Work work = workService.getById(id);
            if (work == null) {
                return Response.error("视频不存在");
            }
            return Response.success(work);
        } catch (Exception e) {
            log.error("获取视频详情失败", e);
            return Response.error("获取视频详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户视频列表
     */
    @GetMapping("/user/{userId}")
    public Response<List<Work>> getUserWorks(@PathVariable Long userId) {
        try {
            List<Work> works = workService.getUserWorks(userId);
            return Response.success(works);
        } catch (Exception e) {
            log.error("获取用户视频失败", e);
            return Response.error("获取用户视频失败: " + e.getMessage());
        }
    }

    /**
     * 获取朋友视频列表
     */
    @GetMapping("/friends/{userId}")
    public Response<List<Work>> getFriendsWorks(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> friends = followService.getMutualFriends(userId);
            if (friends.isEmpty()) {
                return Response.success(new ArrayList<>());
            }
            List<Long> friendIds = new ArrayList<>();
            for (Map<String, Object> friend : friends) {
                friendIds.add(((Number) friend.get("id")).longValue());
            }
            List<Work> works = workService.getWorksByUserIds(friendIds);
            return Response.success(works);
        } catch (Exception e) {
            log.error("获取朋友视频失败", e);
            return Response.error("获取朋友视频失败: " + e.getMessage());
        }
    }

    /**
     * 获取关注用户视频列表
     */
    @GetMapping("/following/{userId}")
    public Response<List<Work>> getFollowingWorks(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> following = followService.getFollowingList(userId);
            if (following.isEmpty()) {
                return Response.success(new ArrayList<>());
            }
            List<Long> followingIds = new ArrayList<>();
            for (Map<String, Object> user : following) {
                followingIds.add(((Number) user.get("id")).longValue());
            }
            List<Work> works = workService.getWorksByUserIds(followingIds);
            return Response.success(works);
        } catch (Exception e) {
            log.error("获取关注视频失败", e);
            return Response.error("获取关注视频失败: " + e.getMessage());
        }
    }

    /**
     * 发布视频
     */
    @PostMapping
    public Response<Work> publishWork(@RequestBody Work work, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);
            if (userId == null) {
                return Response.error(401, "未登录");
            }
            work.setUserId(userId);
            work.setType(2); // 2表示视频
            boolean success = workService.publishWork(work);
            if (success) {
                return Response.success("发布成功", work);
            }
            return Response.error("发布失败");
        } catch (Exception e) {
            log.error("发布视频失败", e);
            return Response.error("发布视频失败: " + e.getMessage());
        }
    }

    /**
     * 删除视频
     */
    @DeleteMapping("/{id}")
    public Response<String> deleteWork(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);
            if (userId == null) {
                return Response.error(401, "未登录");
            }
            Work work = workService.getById(id);
            if (work == null) {
                return Response.error("视频不存在");
            }
            if (!work.getUserId().equals(userId)) {
                return Response.error(403, "无权删除");
            }
            boolean success = workService.deleteWorkWithLikes(id);
            if (success) {
                return Response.success("删除成功", null);
            }
            return Response.error("删除失败");
        } catch (Exception e) {
            log.error("删除视频失败", e);
            return Response.error("删除视频失败: " + e.getMessage());
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

    /**
     * 获取推荐视频列表
     */
    @GetMapping("/recommend")
    public Response<List<Work>> getRecommendWorks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);
            List<Work> works = workService.getRecommendWorks(userId, page, size);
            return Response.success(works);
        } catch (Exception e) {
            log.error("获取推荐视频失败", e);
            return Response.error("获取推荐视频失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门视频列表
     */
    @GetMapping("/hot")
    public Response<List<Work>> getHotWorks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<Work> works = workService.getHotWorks(page, size);
            return Response.success(works);
        } catch (Exception e) {
            log.error("获取热门视频失败", e);
            return Response.error("获取热门视频失败: " + e.getMessage());
        }
    }

    /**
     * 记录观看历史
     */
    @PostMapping("/watch")
    public Response<String> recordWatchHistory(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);
            if (userId == null) {
                return Response.success("未登录，跳过记录");
            }
            Long workId = ((Number) body.get("workId")).longValue();
            Integer watchDuration = body.get("watchDuration") != null ? ((Number) body.get("watchDuration")).intValue() : 0;
            Boolean isComplete = body.get("isComplete") != null ? (Boolean) body.get("isComplete") : false;
            
            workService.incrementViews(workId);
            watchHistoryService.saveWatchHistory(userId, workId, watchDuration, isComplete);
            return Response.success("记录成功");
        } catch (Exception e) {
            log.error("记录观看历史失败", e);
            return Response.error("记录观看历史失败: " + e.getMessage());
        }
    }
}
