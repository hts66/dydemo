package com.example.dyhouduan.service;

import com.example.dyhouduan.entity.Work;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 视频标签分析服务 — 通过 DeepSeek 分析视频内容并生成结构化标签
 */
@Slf4j
@Service
public class VideoTagService {

    private final DeepSeekService deepSeekService;
    private final ObjectMapper mapper;

    public VideoTagService(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
        this.mapper = new ObjectMapper();
    }

    /** 分析结果 */
    public record TagResult(List<String> tags, float[] vector, String rawAnalysis) {}

    /**
     * 分析单个视频，返回标签 + 向量
     */
    public TagResult analyze(Work work) {
        String title = work.getTitle() != null ? work.getTitle() : "";
        String desc = work.getDescription() != null ? work.getDescription() : "";
        String content = title;
        if (!desc.isEmpty() && !desc.equals(title)) {
            content = title + "\n" + desc;
        }
        if (content.isBlank()) {
            content = "(无描述)";
        }

        String prompt = buildPrompt(content);
        String response = deepSeekService.chat(prompt);

        List<String> tags = parseTags(response);
        float[] vector = TagVocabulary.buildVector(tags);

        log.info("视频 [{}] \"{}\" → {} 个标签", work.getId(),
                content.length() > 40 ? content.substring(0, 40) + "..." : content, tags.size());

        return new TagResult(tags, vector, response);
    }

    /** 构建分析 prompt */
    private String buildPrompt(String videoText) {
        return """
你是视频内容分类专家。根据视频标题和描述推断视频内容，从下方标签列表选择匹配的标签，返回JSON数组。

分类步骤：
第1步-选大类(必选1个)：根据标题关键词判断——美女/风景/美食/宠物/街拍/搞笑/教程/音乐/舞蹈/其他
  - 含歌曲名/DJ/歌手/乐队/rap → 音乐
  - 含教程/教学/剪辑/调色 → 教程
  - 含段子/搞笑/精神状态 → 搞笑
  - 含风景/旅行/山河/海/日落 → 风景
  - 含宠物/猫/狗 → 宠物
  - 含舞蹈/舞 → 舞蹈
  - 含穿搭/变装/姐姐/妹妹/少女/丝袜 → 美女
第2步-细分类(选匹配的子标签)：
  - 若是美女：每个子类至少选1个(类型/发型/发色/腿型/丝袜/眼镜/穿搭/身材/年龄感)
  - 若是风景：选风景类型(可多选)
  - 若是其他类：仅返回大类标签即可

标签列表(仅可从中选择)：
%s

视频内容: %s

直接返回JSON数组，无需解释。""".formatted(TagVocabulary.allTagsForPrompt(), videoText);
    }

    /** 解析 DeepSeek 返回的 JSON 数组 */
    private List<String> parseTags(String response) {
        try {
            String trimmed = response.trim();
            // 处理可能的 markdown 代码块包裹
            if (trimmed.startsWith("```")) {
                int start = trimmed.indexOf("[");
                int end = trimmed.lastIndexOf("]");
                if (start >= 0 && end > start) {
                    trimmed = trimmed.substring(start, end + 1);
                }
            }
            return mapper.readValue(trimmed, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析标签失败，尝试提取: {}", response.substring(0, Math.min(200, response.length())));
            return extractTagsFallback(response);
        }
    }

    /** 回退方案：直接从文本中提取已知标签 */
    private List<String> extractTagsFallback(String text) {
        List<String> found = new ArrayList<>();
        // 先检测大类
        String[] categories = {"美女", "风景", "美食", "宠物", "街拍", "搞笑", "教程", "音乐", "舞蹈", "其他"};
        for (String cat : categories) {
            if (text.contains(cat)) {
                found.add(cat);
                break;
            }
        }
        // 扫描所有已知标签
        for (int i = 10; i < TagVocabulary.VECTOR_SIZE; i++) {
            TagVocabulary.TagDef def = null;
            try {
                java.lang.reflect.Field[] fields = TagVocabulary.class.getDeclaredFields();
                for (var f : fields) {
                    if (f.getType() == TagVocabulary.TagDef.class) {
                        TagVocabulary.TagDef t = (TagVocabulary.TagDef) f.get(null);
                        if (t.dim() == i) { def = t; break; }
                    }
                }
            } catch (Exception ignored) {}
            if (def != null && text.contains(def.label())) {
                found.add(def.label());
            }
        }
        return found;
    }
}
