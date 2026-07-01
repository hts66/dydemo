package com.example.dyhouduan.service;

import com.example.dyhouduan.entity.WatchHistory;

import java.util.List;

public interface WatchHistoryService {

    void saveWatchHistory(Long userId, Long workId, Integer watchDuration, Boolean isComplete);

    List<Long> getWatchedWorkIds(Long userId, int limit);

    boolean isWatched(Long userId, Long workId);
}
