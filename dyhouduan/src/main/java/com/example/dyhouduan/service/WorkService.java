package com.example.dyhouduan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dyhouduan.entity.Work;

import java.util.List;

public interface WorkService extends IService<Work> {

    List<Work> getWorksWithUser(int page, int size);

    List<Work> getUserWorks(Long userId);

    List<Work> getWorksByUserIds(List<Long> userIds);

    boolean publishWork(Work work);

    boolean deleteWorkWithLikes(Long id);

    List<Work> getHotWorks(int page, int size);

    List<Work> getRecommendWorks(Long userId, int page, int size);

    void incrementViews(Long workId);

    int batchPublishWorks(Long userId, List<Work> works);

    List<Work> searchWorks(String keyword, int page, int size);
}
