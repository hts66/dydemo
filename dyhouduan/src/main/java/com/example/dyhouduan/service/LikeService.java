package com.example.dyhouduan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dyhouduan.entity.Like;

public interface LikeService extends IService<Like> {

    boolean toggleLike(Long userId, Long workId);

    boolean isLiked(Long userId, Long workId);
}
