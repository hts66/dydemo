package com.example.dyhouduan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dyhouduan.entity.Like;
import com.example.dyhouduan.entity.Work;
import com.example.dyhouduan.mapper.LikeMapper;
import com.example.dyhouduan.mapper.WorkMapper;
import com.example.dyhouduan.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            Work work = workMapper.selectById(workId);
            if (work != null) {
                work.setLikesCount(Math.max(0, work.getLikesCount() - 1));
                workMapper.updateById(work);
            }
            return false;
        } else {
            Like like = new Like();
            like.setUserId(userId);
            like.setWorkId(workId);
            save(like);
            Work work = workMapper.selectById(workId);
            if (work != null) {
                work.setLikesCount(work.getLikesCount() + 1);
                workMapper.updateById(work);
            }
            return true;
        }
    }

    @Override
    public boolean isLiked(Long userId, Long workId) {
        return baseMapper.selectByUserAndWork(userId, workId) != null;
    }
}
