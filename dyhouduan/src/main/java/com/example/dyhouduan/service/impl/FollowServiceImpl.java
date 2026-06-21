package com.example.dyhouduan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dyhouduan.entity.Follow;
import com.example.dyhouduan.mapper.FollowMapper;
import com.example.dyhouduan.service.FollowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    @Override
    @Transactional
    public boolean toggleFollow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            return false;
        }
        Follow existing = baseMapper.selectByFollowerAndFollowee(followerId, followeeId);
        if (existing != null) {
            removeById(existing.getId());
            return false;
        } else {
            Follow follow = new Follow();
            follow.setFollowerId(followerId);
            follow.setFolloweeId(followeeId);
            save(follow);
            return true;
        }
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        return baseMapper.selectByFollowerAndFollowee(followerId, followeeId) != null;
    }

    @Override
    public List<Map<String, Object>> getFollowingList(Long userId) {
        return baseMapper.selectFollowingList(userId);
    }

    @Override
    public List<Map<String, Object>> getFollowerList(Long userId) {
        return baseMapper.selectFollowerList(userId);
    }
}
