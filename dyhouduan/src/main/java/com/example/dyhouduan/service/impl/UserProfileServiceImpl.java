package com.example.dyhouduan.service.impl;

import com.example.dyhouduan.service.QdrantService;
import com.example.dyhouduan.service.TagVocabulary;
import com.example.dyhouduan.service.TagVocabulary.TagDef;
import com.example.dyhouduan.service.UserProfileService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserProfileServiceImpl implements UserProfileService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private QdrantService qdrantService;

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String PREF_KEY_PREFIX = "user:pref:";
    private static final String COUNT_KEY_PREFIX = "user:pref:count:";
    private static final int VECTOR_SIZE = TagVocabulary.VECTOR_SIZE;
    // 30 天过期，长时间不登录自动清理
    private static final int TTL_DAYS = 30;

    @Override
    public void updatePreference(Long userId, Long workId, boolean isLike) {
        try {
            // 1. 从 Qdrant 获取视频的标签向量
            float[] videoVec = qdrantService.getPointVector(workId);
            if (videoVec == null) {
                log.warn("视频 {} 未向量化，跳过偏好更新", workId);
                return;
            }

            String prefKey = PREF_KEY_PREFIX + userId;
            String countKey = COUNT_KEY_PREFIX + userId;

            // 2. 读取当前偏好向量
            float[] currentPref = getPreferenceVector(userId);
            String countStr = redisTemplate.opsForValue().get(countKey);
            int count = countStr != null ? Integer.parseInt(countStr) : 0;

            float[] newPref = new float[VECTOR_SIZE];

            if (isLike) {
                // 点赞：运行加权平均
                for (int i = 0; i < VECTOR_SIZE; i++) {
                    newPref[i] = (currentPref[i] * count + videoVec[i]) / (count + 1);
                }
                count++;
            } else {
                // 取消点赞：反向计算
                if (count <= 1) {
                    newPref = new float[VECTOR_SIZE]; // 归零
                    count = 0;
                } else {
                    for (int i = 0; i < VECTOR_SIZE; i++) {
                        newPref[i] = Math.max(0,
                                (currentPref[i] * count - videoVec[i]) / (count - 1));
                    }
                    count--;
                }
            }

            // 3. 保存回 Redis
            redisTemplate.opsForValue().set(prefKey, toJson(newPref), TTL_DAYS, TimeUnit.DAYS);
            redisTemplate.opsForValue().set(countKey, String.valueOf(count), TTL_DAYS, TimeUnit.DAYS);

            log.info("用户 {} 偏好已更新: like={}, count={}, topTags={}",
                    userId, isLike, count, getTopTags(userId, 5));

        } catch (Exception e) {
            log.error("更新用户偏好失败: userId={}, workId={}", userId, workId, e);
        }
    }

    @Override
    public float[] getPreferenceVector(Long userId) {
        String json = redisTemplate.opsForValue().get(PREF_KEY_PREFIX + userId);
        if (json == null || json.isEmpty()) {
            return new float[VECTOR_SIZE]; // 冷启动：全零向量
        }
        return fromJson(json);
    }

    @Override
    public String getPreferenceAsJson(Long userId) {
        float[] vec = getPreferenceVector(userId);
        return toJson(vec);
    }

    @Override
    public List<String> getTopTags(Long userId, int topN) {
        float[] vec = getPreferenceVector(userId);

        // 按权重排序取 topN
        List<Map.Entry<Integer, Float>> entries = new ArrayList<>();
        for (int i = 0; i < vec.length; i++) {
            if (vec[i] > 0) {
                entries.add(new AbstractMap.SimpleEntry<>(i, vec[i]));
            }
        }
        entries.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

        List<String> tags = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, entries.size()); i++) {
            TagDef def = TagVocabulary.byDim(entries.get(i).getKey());
            if (def != null) {
                tags.add(def.label());
            }
        }
        return tags;
    }

    // ==================== 序列化辅助 ====================

    private String toJson(float[] vec) {
        try {
            List<Double> list = new ArrayList<>();
            for (float v : vec) list.add((double) Math.round(v * 100.0) / 100.0);
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private float[] fromJson(String json) {
        try {
            List<Double> list = mapper.readValue(json, new TypeReference<List<Double>>() {});
            float[] vec = new float[VECTOR_SIZE];
            int len = Math.min(list.size(), VECTOR_SIZE);
            for (int i = 0; i < len; i++) {
                vec[i] = list.get(i).floatValue();
            }
            return vec;
        } catch (Exception e) {
            return new float[VECTOR_SIZE];
        }
    }
}
