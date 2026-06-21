package com.example.dyhouduan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dyhouduan.entity.Work;
import com.example.dyhouduan.mapper.WorkMapper;
import com.example.dyhouduan.service.WorkService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkServiceImpl extends ServiceImpl<WorkMapper, Work> implements WorkService {

    @Override
    public List<Work> getWorksWithUser(int page, int size) {
        int offset = (page - 1) * size;
        return baseMapper.selectWorksWithUser(offset, size);
    }

    @Override
    public List<Work> getUserWorks(Long userId) {
        return baseMapper.selectByUserId(userId);
    }

    @Override
    public boolean publishWork(Work work) {
        work.setLikesCount(0);
        work.setCommentsCount(0);
        return save(work);
    }
}
