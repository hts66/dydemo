package com.example.dyhouduan.service;

import com.example.dyhouduan.entity.Work;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 视频推荐服务 — 用户说"推荐XX视频"时，提取标签 + 搜索 Qdrant 返回匹配视频
 */
@Slf4j
@Service
public class VideoRecommendService {

    private final DeepSeekService deepSeekService;
    private final QdrantService qdrantService;
    private final WorkService workService;
    private final ObjectMapper mapper;

    public VideoRecommendService(DeepSeekService deepSeekService, QdrantService qdrantService,
                                  WorkService workService) {
        this.deepSeekService = deepSeekService;
        this.qdrantService = qdrantService;
        this.workService = workService;
        this.mapper = new ObjectMapper();
    }

    /** 推荐结果 */
    public record RecommendResult(String replyText, List<VideoCard> videos) {}

    /** 视频卡片 */
    public record VideoCard(long id, String title, String description, String url,
                            String thumbnail, String username, double score, List<String> tags) {}

    /**
     * 根据用户自然语言查询推荐视频
     */
    public RecommendResult recommend(String userQuery, int limit) {
        try {
            // 1. 用 DeepSeek 从用户查询中提取标签
            List<String> tags = extractTags(userQuery);
            log.info("用户查询: \"{}\" → 提取标签: {}", userQuery, tags);

            if (tags.isEmpty()) {
                return new RecommendResult("抱歉，我没太理解你想看什么类型的视频，可以再说具体一点吗？比如\"推荐御姐黑丝视频\"~", List.of());
            }

            // 2. 构建向量
            float[] vector = TagVocabulary.buildVector(tags);
            if (isAllZero(vector)) {
                return new RecommendResult("抱歉，我暂时没有收录这类视频的标签，换个类型试试？", List.of());
            }

            // 3. 搜索 Qdrant
            List<QdrantService.SearchResult> results = qdrantService.searchSimilar(vector, limit);
            if (results.isEmpty()) {
                return new RecommendResult("抱歉，没有找到匹配「%s」的视频，换个关键词试试吧~".formatted(
                        String.join("、", tags)), List.of());
            }

            // 4. 获取视频详情
            List<VideoCard> cards = new ArrayList<>();
            for (var r : results) {
                Work work = workService.getById(r.id());
                if (work == null) continue;

                Map<String, Object> payload = r.payload();
                @SuppressWarnings("unchecked")
                List<String> workTags = payload.get("tags") instanceof List ?
                        (List<String>) payload.get("tags") : List.of();

                cards.add(new VideoCard(
                        work.getId(),
                        work.getTitle() != null ? work.getTitle() : "",
                        work.getDescription() != null ? work.getDescription() : "",
                        work.getUrl() != null ? work.getUrl() : "",
                        work.getThumbnail() != null ? work.getThumbnail() : "",
                        work.getUsername() != null ? work.getUsername() : "匿名用户",
                        Math.round(r.score() * 100) / 100.0,
                        workTags
                ));
            }

            if (cards.isEmpty()) {
                return new RecommendResult("找到了一些匹配结果，但视频数据不完整，换个关键词试试吧~", List.of());
            }

            // 5. 生成友好的回复
            String reply = buildReply(tags, cards);
            return new RecommendResult(reply, cards);

        } catch (Exception e) {
            log.error("视频推荐失败", e);
            return new RecommendResult("推荐系统暂时出了点小问题，稍后再试吧~", List.of());
        }
    }

    /** 用 DeepSeek 从用户查询中提取标签 */
    private List<String> extractTags(String userQuery) {
        String prompt = """
你是一个视频标签提取器。用户想要搜索视频，请从标签列表中选择最匹配的标签，返回JSON数组。

标签列表: %s

规则:
- 用户可能说"推荐XX视频"、"给我XX的"、"有没有XX"等
- 从标签列表中选择所有符合的标签(大类+子类都要选)
- 如果用户明确说了属性(如"黑丝")，一定要选上
- 如果用户没提的属性不要选
- 只返回JSON数组,如: ["美女","黑丝","长腿"]

用户查询: %s

JSON数组:""".formatted(TagVocabulary.allTagsForPrompt(), userQuery);

        String response = deepSeekService.chat(prompt);
        try {
            String trimmed = response.trim();
            if (trimmed.startsWith("```")) {
                int s = trimmed.indexOf("[");
                int e = trimmed.lastIndexOf("]");
                if (s >= 0 && e > s) trimmed = trimmed.substring(s, e + 1);
            }
            return mapper.readValue(trimmed, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("标签提取解析失败: {}", response.substring(0, Math.min(200, response.length())));
            return List.of();
        }
    }

    private boolean isAllZero(float[] vec) {
        for (float v : vec) if (v != 0) return false;
        return true;
    }

    private String buildReply(List<String> tags, List<VideoCard> cards) {
        String tagStr = String.join("、", tags);
        return "为你找到了 %d 个「%s」视频，快来看看吧~ 🎬".formatted(cards.size(), tagStr);
    }
}
