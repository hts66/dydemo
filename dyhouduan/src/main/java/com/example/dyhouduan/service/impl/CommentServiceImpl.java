package com.example.dyhouduan.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dyhouduan.entity.Comment;
import com.example.dyhouduan.entity.Work;
import com.example.dyhouduan.mapper.CommentMapper;
import com.example.dyhouduan.mapper.WorkMapper;
import com.example.dyhouduan.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private WorkMapper workMapper;

    @Override
    public List<Comment> getCommentsByWorkId(Long workId) {
        return baseMapper.selectByWorkIdWithUser(workId);
    }

    @Override
    @Transactional
    public boolean addComment(Long userId, Long workId, String content) {
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setWorkId(workId);
        comment.setContent(content);
        boolean success = save(comment);
        if (success) {
            LambdaUpdateWrapper<Work> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Work::getId, workId)
                         .setSql("comments_count = comments_count + 1");
            workMapper.update(null, updateWrapper);
        }
        return success;
    }
}
