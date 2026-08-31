package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.entity.User;
import com.example.dyhouduan.entity.Work;
import com.example.dyhouduan.service.FollowService;
import com.example.dyhouduan.service.UserService;
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
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WatchHistoryService watchHistoryService;

    /**
     * 获取视频列表（分页），可选 keyword 参数进行搜索
     */
    @GetMapping
    public Response<List<Work>> getWorks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean random,
            @RequestParam(defaultValue = "0") int seed) {
        try {
            List<Work> works;
            if (keyword != null && !keyword.trim().isEmpty()) {
                works = workService.searchWorks(keyword.trim(), page, size);
            } else if (random) {
                works = workService.getRandomWorks(page, size, seed);
            } else {
                works = workService.getWorksWithUser(page, size);
            }
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
    public Response<List<Work>> getUserWorks(@PathVariable Long userId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "12") int size) {
        try {
            List<Work> works = workService.getUserWorks(userId, page, size);
            return Response.success(works);
        } catch (Exception e) {
            log.error("获取用户视频失败", e);
            return Response.error("获取用户视频失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户的朋友（互关）视频列表
     * userId 只取自 token，不接受外部传入，避免越权查看他人社交关系
     */
    @GetMapping("/my/friends")
    public Response<List<Work>> getMyFriendsWorks(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);
            if (userId == null) {
                return Response.error(401, "未登录");
            }
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
     * 获取当前登录用户的关注视频列表
     * userId 只取自 token，不接受外部传入，避免越权查看他人社交关系
     */
    @GetMapping("/my/following")
    public Response<List<Work>> getMyFollowingWorks(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);
            if (userId == null) {
                return Response.error(401, "未登录");
            }
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
     * 获取个性化推荐视频列表
     * 未登录用户返回热门视频
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
     * 种子数据批量导入 — 通过邮箱指定发布者
     * Body: { "email": "2703605029@qq.com", "works": [ { "url":"...", "thumbnail":"...", "title":"...", "description":"..." }, ... ] }
     */
    @PostMapping("/seed")
    public Response<Map<String, Object>> seedWorks(@RequestBody Map<String, Object> body) {
        try {
            String email = (String) body.get("email");
            if (email == null || email.isEmpty()) {
                return Response.error("email 不能为空");
            }

            User user = userService.findByEmail(email);
            if (user == null) {
                return Response.error("用户不存在: " + email);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawList = (List<Map<String, Object>>) body.get("works");
            if (rawList == null || rawList.isEmpty()) {
                return Response.error("works 列表不能为空");
            }

            List<Work> works = new ArrayList<>();
            for (Map<String, Object> raw : rawList) {
                Work w = new Work();
                w.setUrl((String) raw.get("url"));
                w.setThumbnail((String) raw.get("thumbnail"));
                w.setTitle((String) raw.get("title"));
                w.setDescription((String) raw.get("description"));
                works.add(w);
            }

            int count = workService.batchPublishWorks(user.getId(), works);

            Map<String, Object> result = Map.of("count", count, "userId", user.getId());
            return Response.success("批量导入成功", result);
        } catch (Exception e) {
            log.error("种子数据导入失败", e);
            return Response.error("导入失败: " + e.getMessage());
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
