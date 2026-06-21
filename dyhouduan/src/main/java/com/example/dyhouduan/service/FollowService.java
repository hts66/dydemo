package com.example.dyhouduan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dyhouduan.entity.Follow;

import java.util.List;
import java.util.Map;

public interface FollowService extends IService<Follow> {

    boolean toggleFollow(Long followerId, Long followeeId);

    boolean isFollowing(Long followerId, Long followeeId);

    List<Map<String, Object>> getFollowingList(Long userId);

    List<Map<String, Object>> getFollowerList(Long userId);
}
