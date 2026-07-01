package com.example.dyhouduan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dyhouduan.entity.WatchHistory;
import com.example.dyhouduan.mapper.WatchHistoryMapper;
import com.example.dyhouduan.service.WatchHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WatchHistoryServiceImpl extends ServiceImpl<WatchHistoryMapper, WatchHistory> implements WatchHistoryService {

    @Autowired
    private WatchHistoryMapper watchHistoryMapper;

    @Override
    public void saveWatchHistory(Long userId, Long workId, Integer watchDuration, Boolean isComplete) {
        WatchHistory history = new WatchHistory();
        history.setUserId(userId);
        history.setWorkId(workId);
        history.setWatchDuration(watchDuration);
        history.setIsComplete(isComplete);
        history.setCreatedAt(LocalDateTime.now());
        save(history);
    }

    @Override
    public List<Long> getWatchedWorkIds(Long userId, int limit) {
        return watchHistoryMapper.selectWatchedWorkIds(userId, limit);
    }

    @Override
    public boolean isWatched(Long userId, Long workId) {
        return watchHistoryMapper.countByUserIdAndWorkId(userId, workId) > 0;
    }
}
