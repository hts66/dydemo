package com.example.dyhouduan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dyhouduan.entity.Like;
import com.example.dyhouduan.entity.Work;
import com.example.dyhouduan.mapper.LikeMapper;
import com.example.dyhouduan.mapper.WorkMapper;
import com.example.dyhouduan.service.WorkService;
import com.example.dyhouduan.config.OssProperties;
import com.example.dyhouduan.utils.FileStorageUtil;
import com.example.dyhouduan.utils.OssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkServiceImpl extends ServiceImpl<WorkMapper, Work> implements WorkService {

    @Autowired
    private LikeMapper likeMapper;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private FileStorageUtil fileStorageUtil;

    @Autowired
    private OssProperties ossProperties;

    private boolean useOss() {
        return ossProperties.getEndpoint() != null && !ossProperties.getEndpoint().isEmpty();
    }

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
    public List<Work> getWorksByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return baseMapper.selectByUserIds(userIds);
    }

    @Override
    public List<Work> getHotWorks(int page, int size) {
        int offset = (page - 1) * size;
        return baseMapper.selectHotWorks(offset, size);
    }

    @Override
    public List<Work> getRecommendWorks(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        // 未登录用户没有个性化数据，直接返回热门作品
        if (userId == null) {
            return baseMapper.selectHotWorks(offset, size);
        }
        List<Work> works = baseMapper.selectRecommendWorks(userId, offset, size);
        // 个性化结果不足（例如新用户已看完可推荐内容）时，用热门作品兜底
        if (works.isEmpty() && page == 1) {
            return baseMapper.selectHotWorks(offset, size);
        }
        return works;
    }

    @Override
    public void incrementViews(Long workId) {
        Work work = getById(workId);
        if (work != null) {
            work.setViews(work.getViews() + 1);
            updateById(work);
        }
    }

    @Override
    public boolean publishWork(Work work) {
        work.setLikesCount(0);
        work.setCommentsCount(0);
        work.setViews(0);
        return save(work);
    }

    @Override
    @Transactional
    public int batchPublishWorks(Long userId, List<Work> works) {
        for (Work work : works) {
            work.setUserId(userId);
            work.setType(2);
            work.setLikesCount(0);
            work.setCommentsCount(0);
            work.setViews(0);
        }
        saveBatch(works);
        return works.size();
    }

    @Override
    @Transactional
    public boolean deleteWorkWithLikes(Long id) {
        Work work = getById(id);
        if (work == null) {
            return false;
        }

        if (useOss()) {
            ossUtil.deleteFile(work.getUrl());
            ossUtil.deleteFile(work.getThumbnail());
        } else {
            fileStorageUtil.deleteFile(work.getUrl());
            fileStorageUtil.deleteFile(work.getThumbnail());
        }

        LambdaQueryWrapper<Like> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(Like::getWorkId, id);
        likeMapper.delete(likeWrapper);
        
        return removeById(id);
    }
}
