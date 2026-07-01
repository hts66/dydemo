package com.example.dyhouduan.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dyhouduan.entity.Follow;
import com.example.dyhouduan.entity.User;
import com.example.dyhouduan.mapper.FollowMapper;
import com.example.dyhouduan.mapper.UserMapper;
import com.example.dyhouduan.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public boolean toggleFollow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            return false;
        }
        Follow existing = baseMapper.selectByFollowerAndFollowing(followerId, followingId);
        if (existing != null) {
            removeById(existing.getId());
            LambdaUpdateWrapper<User> followerUpdate = new LambdaUpdateWrapper<>();
            followerUpdate.eq(User::getId, followerId)
                          .setSql("following_count = GREATEST(0, following_count - 1)");
            userMapper.update(null, followerUpdate);
            LambdaUpdateWrapper<User> followingUpdate = new LambdaUpdateWrapper<>();
            followingUpdate.eq(User::getId, followingId)
                           .setSql("followers_count = GREATEST(0, followers_count - 1)");
            userMapper.update(null, followingUpdate);
            return false;
        } else {
            Follow follow = new Follow();
            follow.setFollowerId(followerId);
            follow.setFollowingId(followingId);
            save(follow);
            LambdaUpdateWrapper<User> followerUpdate = new LambdaUpdateWrapper<>();
            followerUpdate.eq(User::getId, followerId)
                          .setSql("following_count = following_count + 1");
            userMapper.update(null, followerUpdate);
            LambdaUpdateWrapper<User> followingUpdate = new LambdaUpdateWrapper<>();
            followingUpdate.eq(User::getId, followingId)
                           .setSql("followers_count = followers_count + 1");
            userMapper.update(null, followingUpdate);
            return true;
        }
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        return baseMapper.selectByFollowerAndFollowing(followerId, followingId) != null;
    }

    @Override
    public List<Map<String, Object>> getFollowingList(Long userId) {
        return baseMapper.selectFollowingList(userId);
    }

    @Override
    public List<Map<String, Object>> getFollowerList(Long userId) {
        return baseMapper.selectFollowerList(userId);
    }

    @Override
    public List<Map<String, Object>> getMutualFriends(Long userId) {
        return baseMapper.selectMutualFriends(userId);
    }
}
