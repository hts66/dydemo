package com.example.dyhouduan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dyhouduan.entity.Like;
import com.example.dyhouduan.entity.Work;
import com.example.dyhouduan.mapper.LikeMapper;
import com.example.dyhouduan.mapper.WorkMapper;
import com.example.dyhouduan.service.WorkService;
import com.example.dyhouduan.config.MinioProperties;
import com.example.dyhouduan.utils.FileStorageUtil;
import com.example.dyhouduan.utils.MinioUtil;
import com.example.dyhouduan.service.QdrantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class WorkServiceImpl extends ServiceImpl<WorkMapper, Work> implements WorkService {

    @Autowired
    private LikeMapper likeMapper;

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private FileStorageUtil fileStorageUtil;

    @Autowired
    private MinioProperties minioProperties;

    @Autowired
    private QdrantService qdrantService;

    // ── 全量作品 ID 缓存：应用层洗牌用。发布/删除时主动失效，60s TTL 兜底 ──
    private static final long ID_CACHE_TTL_MS = 60_000;
    private volatile List<Long> allWorkIds = null;
    private volatile long allWorkIdsCachedAt = 0;

    private List<Long> getAllWorkIds() {
        List<Long> cached = allWorkIds;
        if (cached != null && System.currentTimeMillis() - allWorkIdsCachedAt < ID_CACHE_TTL_MS) {
            return cached;
        }
        cached = baseMapper.selectAllIds();
        allWorkIds = cached;
        allWorkIdsCachedAt = System.currentTimeMillis();
        return cached;
    }

    private void invalidateWorkIdCache() {
        allWorkIds = null;
    }

    private boolean useMinio() {
        return minioProperties.getEndpoint() != null && !minioProperties.getEndpoint().isEmpty();
    }

    @Override
    public List<Work> getWorksWithUser(int page, int size) {
        int offset = (page - 1) * size;
        return baseMapper.selectWorksWithUser(offset, size);
    }

    @Override
    public List<Work> getUserWorks(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        return baseMapper.selectByUserId(userId, offset, size);
    }

    @Override
    public List<Work> getWorksByUserIds(List<Long> userIds, int page, int size) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        int offset = (page - 1) * size;
        return baseMapper.selectByUserIds(userIds, offset, size);
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
        boolean ok = save(work);
        if (ok) {
            invalidateWorkIdCache();
        }
        return ok;
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
        invalidateWorkIdCache();
        return works.size();
    }

    @Override
    public List<Work> searchWorks(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        return baseMapper.selectByKeyword(keyword, offset, size);
    }

    @Override
    public List<Work> getRandomWorks(int page, int size, int seed) {
        List<Long> ids = getAllWorkIds();
        if (ids.isEmpty()) {
            return List.of();
        }
        // 确定性洗牌：复制一份后用 seed 洗牌（不污染缓存）。
        // 同一 seed + 同一 id 列表 ⇒ 完全相同排列，保证翻页不重不漏
        List<Long> shuffled = new ArrayList<>(ids);
        Collections.shuffle(shuffled, new Random(seed));

        int from = (page - 1) * size;
        if (from >= shuffled.size()) {
            return List.of();
        }
        List<Long> pageIds = new ArrayList<>(
                shuffled.subList(from, Math.min(from + size, shuffled.size())));
        if (pageIds.isEmpty()) {
            return List.of();
        }

        // 主键点查 + 按洗牌顺序重排（IN 不保证顺序）
        List<Work> works = baseMapper.selectByIds(pageIds);
        Map<Long, Work> byId = new HashMap<>();
        for (Work w : works) {
            byId.put(w.getId(), w);
        }
        List<Work> ordered = new ArrayList<>(pageIds.size());
        for (Long id : pageIds) {
            Work w = byId.get(id);
            if (w != null) {
                ordered.add(w);
            }
        }
        return ordered;
    }

    @Override
    @Transactional
    public boolean deleteWorkWithLikes(Long id) {
        Work work = getById(id);
        if (work == null) {
            return false;
        }

        if (useMinio()) {
            minioUtil.deleteFile(work.getUrl());
            minioUtil.deleteFile(work.getThumbnail());
        } else {
            fileStorageUtil.deleteFile(work.getUrl());
            fileStorageUtil.deleteFile(work.getThumbnail());
        }

        LambdaQueryWrapper<Like> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(Like::getWorkId, id);
        likeMapper.delete(likeWrapper);

        // 同步删除 Qdrant 向量，避免 AI 推荐已删除的视频
        qdrantService.deletePoint(id);

        boolean ok = removeById(id);
        if (ok) {
            invalidateWorkIdCache();
        }
        return ok;
    }
}
