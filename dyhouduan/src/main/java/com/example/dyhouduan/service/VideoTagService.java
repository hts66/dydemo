package com.example.dyhouduan.service;

import com.example.dyhouduan.entity.Work;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

/**
 * 视频标签分析服务
 *
 * 方案: 封面图视觉分析 → 文本降级
 *   1. 下载视频封面图（MinIO 本地，瞬间完成）
 *   2. Base64编码 → 发送给通义千问 Vision API
 *   3. LLM分析画面内容 → 返回标签列表 + 128维向量
 *   4. 降级: 无封面或视觉失败 → 标题/描述文本分析
 *
 * 规则: 画面内容与子类标签不符合时只标记大类，子类全部跳过
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
    public record TagResult(List<String> tags, float[] vector, String rawAnalysis, String source) {}

    /** 视觉分析最大重试次数 */
    private static final int VISION_MAX_RETRIES = 2;

    /**
     * 分析单个视频
     * 优先: 封面图 → 通义千问VL视觉识别
     * 降级: 标题+描述 → DeepSeek文本分析
     */
    public TagResult analyze(Work work) {
        String title = work.getTitle() != null ? work.getTitle() : "";
        String desc = work.getDescription() != null ? work.getDescription() : "";
        String thumbnailUrl = work.getThumbnail();

        // 尝试封面图视觉分析（有封面且是HTTP URL）
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()
                && (thumbnailUrl.startsWith("http://") || thumbnailUrl.startsWith("https://"))) {
            for (int attempt = 1; attempt <= VISION_MAX_RETRIES; attempt++) {
                try {
                    TagResult result = analyzeByCover(thumbnailUrl, title, desc, work.getId());
                    log.info("🖼️ [封面视觉] workId={} → {} (尝试{}/{})",
                            work.getId(), result.tags(), attempt, VISION_MAX_RETRIES);
                    return result;
                } catch (Exception e) {
                    if (attempt < VISION_MAX_RETRIES) {
                        log.warn("⚠️ 封面分析失败 workId={}, 第{}次重试: {}",
                                work.getId(), attempt, e.getMessage());
                        try { Thread.sleep(1000L * attempt); } catch (InterruptedException ignored) {}
                    } else {
                        log.warn("⚠️ 封面分析{}次均失败 workId={}, 降级文本: {}",
                                VISION_MAX_RETRIES, work.getId(), e.getMessage());
                    }
                }
            }
        }

        // 降级：纯文本分析
        TagResult result = analyzeByText(title, desc, work.getId());
        log.info("📝 [文本] workId={} → {}", work.getId(), result.tags());
        return result;
    }

    // ==================== 封面图视觉分析 ====================

    private TagResult analyzeByCover(String coverUrl, String title, String desc, Long workId) throws Exception {
        // 1. 下载封面图
        byte[] imageBytes;
        try {
            imageBytes = downloadImage(coverUrl);
        } catch (Exception e) {
            throw new RuntimeException("封面图下载失败: " + e.getMessage(), e);
        }

        if (imageBytes == null || imageBytes.length < 100) {
            throw new RuntimeException("封面图数据异常(size=" + (imageBytes == null ? 0 : imageBytes.length) + ")");
        }

        log.info("📥 封面下载完成 workId={}, size={}KB", workId, imageBytes.length / 1024);

        // 2. Base64 编码
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // 3. 构建封面分析 prompt
        String prompt = buildCoverPrompt(title, desc);

        // 4. 调用通义千问 Vision API
        String response = deepSeekService.chatVision(prompt, List.of(base64Image));

        // 5. 解析标签
        List<String> tags = parseTags(response);
        if (tags.isEmpty()) {
            tags = extractTagsFromText(title, desc);
        }
        float[] vector = TagVocabulary.buildVector(tags);

        log.info("🖼️ 封面分析 workId={}: {} → {} 标签", workId,
                title.length() > 30 ? title.substring(0, 30) + "..." : title, tags);

        return new TagResult(tags, vector, response, "vision");
    }

    /** 下载图片 */
    private byte[] downloadImage(String url) throws Exception {
        try (InputStream in = new URL(url).openStream()) {
            return in.readAllBytes();
        }
    }

    /** 封面图视觉分析 prompt */
    private String buildCoverPrompt(String title, String desc) {
        String titleInfo = (title != null && !title.isBlank())
                ? "\n视频标题: " + title.substring(0, Math.min(100, title.length())) : "";
        String descInfo = (desc != null && !desc.isBlank() && !desc.equals(title))
                ? "\n视频描述: " + desc.substring(0, Math.min(100, desc.length())) : "";

        return """
你是一个视频内容分析专家。我会给你一张视频的封面图，请你仔细观察画面内容，判断视频属于哪个大类，以及画面中具体出现了什么。""" + titleInfo + descInfo + """

## 大类(三选一)
- 美女: 画面主体是一个或多个女性人物
- 小猫: 画面主体是一只或多只猫
- 风景: 画面主体是自然/城市景观，没有突出的人物或动物

## 子类标签(选填)
如果大类是美女，从以下子类选择（画面清晰时尽量选，每个子分类至少选1个最像的）：
  类型: 御姐 萝莉 甜妹 酷飒 清新
  发型: 长发 短发 中长发
  发色: 黑发 棕发 金发 其他发色
  腿型: 长腿 肉腿 普通腿型
  丝袜: 光腿 黑丝 白丝 肉丝 渔网 其他丝袜
  眼镜: 眼镜妹 无眼镜
  穿搭: 连衣裙 JK制服 汉服 旗袍 吊带 运动装 休闲装 性感穿搭 可爱风 职场装
  身材: 苗条 丰满 骨感
  年龄感: 少女感 轻熟 成熟
  动作: 跳舞

如果大类是小猫，从以下子类选择（画面清晰时尽量选，每个子分类至少选1个）：
  品种: 橘猫 英短 美短 布偶猫 暹罗猫 田园猫 其他猫
  颜色: 橘色 白色 黑色 灰色 三花 狸花
  年龄: 幼猫 成年猫
  行为: 玩耍 睡觉 吃饭 卖萌 舔毛
  数量: 单只 多只
  场景: 室内 室外

如果大类是风景，从以下子类选择（画面清晰时尽量选，每个子分类至少选1个）：
  类型: 自然风光 城市夜景 海景 山景 日落 雪景 森林 花海 古镇 草原 湖景 瀑布 沙漠 极光 云海 雨景
  拍摄: 航拍 延时 慢动作 常规拍摄
  时间: 白天 夜晚

## 关键规则
1. 首先必须确定大类（美女/小猫/风景三选一），返回标签列表的第一项必须是大类
2. 每个子分类尽量选1个最符合画面内容的标签（即使不完全确定，选最接近的）
3. 只有封面图完全模糊、完全看不清时才只返回大类
4. 对美女视频，特别注意穿搭、发型、发色这三个最容易辨别的特征
5. 如果画面内容与三大类都不符合 → 返回空数组 []
6. 封面图可能包含文字/贴纸，请以画面主体为准，不要被文字误导

## 输出格式
直接返回JSON数组，不要任何解释文字。例如:
["美女","甜妹","长发","黑发","长腿","黑丝","无眼镜","JK制服","苗条","少女感"]
["小猫","橘猫","橘色","成年猫","玩耍","单只","室内"]
["风景","日落","海景","常规拍摄","白天"]
[]
""";
    }

    // ==================== 降级: 文本分析 ====================

    private TagResult analyzeByText(String title, String desc, Long workId) {
        String content = buildTextContent(title, desc);
        String prompt = buildTextPrompt(content);
        String response = deepSeekService.chat(prompt);

        List<String> tags = parseTags(response);
        if (tags.isEmpty()) {
            tags = extractTagsFromText(title, desc);
        }
        float[] vector = TagVocabulary.buildVector(tags);

        log.info("📝 文本分析 workId={}: → {} 标签", workId, tags);
        return new TagResult(tags, vector, response, "text");
    }

    private String buildTextContent(String title, String desc) {
        if (desc != null && !desc.isBlank() && !desc.equals(title)) {
            return title + "\n" + desc;
        }
        return title != null && !title.isBlank() ? title : "(无描述)";
    }

    private String buildTextPrompt(String content) {
        return """
你是视频内容分析专家。根据视频标题和描述推断视频内容，从下方标签列表选择匹配的标签，返回JSON数组。

## 分类步骤
第1步-判断大类(必须选择一个): 美女/小猫/风景
  - 含美女/姐姐/妹妹/少女/御姐/穿搭/丝袜/舞蹈 → 美女
  - 含猫/猫咪/小猫/萌宠/喵 → 小猫
  - 含风景/旅行/山河/海/日落/山川/云海/自然 → 风景
  - 完全无法判断类别 → 返回 []

第2步-细分类(选匹配的子标签，不确定的就不要选):
  - 只选择标题/描述中明确提到的子标签
  - 描述中没提到的属性不要乱猜

## 标签列表
%s

## 视频内容
%s

## 输出格式
直接返回JSON数组，无需解释。首项必须是大类标签。
例如: ["美女","黑丝","长腿"] 或 ["小猫","橘猫"] 或 ["风景","日落"]
""".formatted(TagVocabulary.allTagsForPrompt(), content);
    }

    /** 从文本中提取已知标签(降级兜底) */
    private List<String> extractTagsFromText(String title, String desc) {
        List<String> found = new ArrayList<>();
        String text = (title + " " + (desc != null ? desc : "")).toLowerCase();

        // 大类检测
        if (text.contains("美女") || text.contains("姐姐") || text.contains("妹妹")
                || text.contains("少女") || text.contains("御姐") || text.contains("穿搭")
                || text.contains("丝袜")) {
            found.add("美女");
        } else if (text.contains("猫") || text.contains("喵") || text.contains("萌宠")) {
            found.add("小猫");
        } else if (text.contains("风景") || text.contains("旅行") || text.contains("日落")
                || text.contains("海") || text.contains("山") || text.contains("自然")) {
            found.add("风景");
        }

        // 子标签扫描
        for (int i = 1; i < TagVocabulary.VECTOR_SIZE; i++) {
            TagVocabulary.TagDef def = TagVocabulary.byDim(i);
            if (def != null && text.contains(def.label())) {
                found.add(def.label());
            }
        }
        return found;
    }

    // ==================== 通用: 解析标签 ====================

    /** 解析 LLM 返回的 JSON 数组，过滤掉不在标签库中的词 */
    private List<String> parseTags(String response) {
        List<String> raw;
        try {
            String trimmed = response.trim();
            // 处理 markdown 代码块包裹
            if (trimmed.startsWith("```")) {
                int start = trimmed.indexOf("[");
                int end = trimmed.lastIndexOf("]");
                if (start >= 0 && end > start) {
                    trimmed = trimmed.substring(start, end + 1);
                }
            }
            raw = mapper.readValue(trimmed, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析标签JSON失败，尝试从文本提取: {}",
                    response.length() > 150 ? response.substring(0, 150) + "..." : response);
            return extractTagsFromText(response, "");
        }

        // 过滤: 只保留在 TagVocabulary 中定义的标签（拒绝LLM编造的词）
        List<String> filtered = new ArrayList<>();
        for (String tag : raw) {
            String t = tag.trim();
            if (TagVocabulary.isCategoryTag(t) || TagVocabulary.find(t) != null) {
                filtered.add(t);
            } else {
                log.info("⚠️ 过滤不在标签库中的词: \"{}\"", t);
            }
        }
        return filtered;
    }
}
