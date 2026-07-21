package com.example.dyhouduan.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dyhouduan.entity.Like;
import com.example.dyhouduan.entity.Work;
import com.example.dyhouduan.mapper.LikeMapper;
import com.example.dyhouduan.mapper.WorkMapper;
import com.example.dyhouduan.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like> implements LikeService {

    @Autowired
    private WorkMapper workMapper;

    @Override
    @Transactional
    public boolean toggleLike(Long userId, Long workId) {
        Like existing = baseMapper.selectByUserAndWork(userId, workId);
        if (existing != null) {
            removeById(existing.getId());
            LambdaUpdateWrapper<Work> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Work::getId, workId)
                         .setSql("likes_count = GREATEST(0, likes_count - 1)");
            workMapper.update(null, updateWrapper);
            return false;
        } else {
            Like like = new Like();
            like.setUserId(userId);
            like.setWorkId(workId);
            save(like);
            LambdaUpdateWrapper<Work> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Work::getId, workId)
                         .setSql("likes_count = likes_count + 1");
            workMapper.update(null, updateWrapper);
            return true;
        }
    }

    @Override
    public boolean isLiked(Long userId, Long workId) {
        return baseMapper.selectByUserAndWork(userId, workId) != null;
    }

    @Override
    public List<Work> getLikedWorks(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        return baseMapper.selectLikedWorks(userId, offset, size);
    }
}
