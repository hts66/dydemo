package com.example.dyhouduan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dyhouduan.entity.Work;

import java.util.List;

public interface WorkService extends IService<Work> {

    List<Work> getWorksWithUser(int page, int size);

    List<Work> getUserWorks(Long userId);

    boolean publishWork(Work work);
}
