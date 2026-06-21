package com.example.dyhouduan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dyhouduan.entity.Comment;

import java.util.List;

public interface CommentService extends IService<Comment> {

    List<Comment> getCommentsByWorkId(Long workId);

    boolean addComment(Long userId, Long workId, String content);
}
