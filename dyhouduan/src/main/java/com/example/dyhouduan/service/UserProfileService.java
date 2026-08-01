package com.example.dyhouduan.service;

import java.util.List;

/**
 * 用户偏好画像服务 — 基于点赞视频的标签向量构建用户偏好
 */
public interface UserProfileService {

    /** 更新用户偏好（点赞/取消点赞时调用） */
    void updatePreference(Long userId, Long workId, boolean isLike);

    /** 获取用户偏好向量（128维 float 数组） */
    float[] getPreferenceVector(Long userId);

    /** 获取用户偏好向量（JSON格式，给前端/AI用） */
    String getPreferenceAsJson(Long userId);

    /** 获取用户点赞过的视频标签列表 */
    List<String> getTopTags(Long userId, int topN);
}
