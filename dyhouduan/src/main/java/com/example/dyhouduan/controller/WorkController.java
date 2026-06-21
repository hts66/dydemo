package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.entity.Work;
import com.example.dyhouduan.service.WorkService;
import com.example.dyhouduan.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/works")
public class WorkController {

    @Autowired
    private WorkService workService;

    @Autowired
    private JwtUtil jwtUtil;

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
            boolean success = workService.removeById(id);
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
}
