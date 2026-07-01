package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.entity.Comment;
import com.example.dyhouduan.service.CommentService;
import com.example.dyhouduan.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/{workId}")
    public Response<List<Comment>> getComments(@PathVariable Long workId) {
        try {
            List<Comment> comments = commentService.getCommentsByWorkId(workId);
            log.info("查询作品[{}]的评论列表，共{}条评论", workId, comments.size());
            return Response.success(comments);
        } catch (Exception e) {
            log.error("获取评论失败", e);
            return Response.error("获取评论失败: " + e.getMessage());
        }
    }

    @PostMapping
    public Response<Comment> addComment(@RequestBody CommentRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            if (userId == null) {
                return Response.error(401, "未登录");
            }
            boolean success = commentService.addComment(userId, request.getWorkId(), request.getContent());
            if (success) {
                log.info("用户[{}]对作品[{}]发表评论：{}", userId, request.getWorkId(), request.getContent());
                return Response.success("评论成功", null);
            }
            return Response.error("评论失败");
        } catch (Exception e) {
            log.error("评论失败", e);
            return Response.error("评论失败: " + e.getMessage());
        }
    }

    public static class CommentRequest {
        private Long workId;
        private String content;

        public Long getWorkId() { return workId; }
        public void setWorkId(Long workId) { this.workId = workId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
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
