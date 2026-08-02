package com.example.dyhouduan.service;

import java.util.*;

/**
 * 视频标签词汇表 — 128维向量，精准度优先简化版
 *
 * 维度分配 (one-hot):
 *   0-2       大类 (美女/小猫/风景)
 *   3-15      穿搭 (13维)
 *   18-21     丝袜 (4维)
 *   22-23     舞蹈 (2维)
 *   24-30     人物 (7维)
 *   31-32     场景 (2维)
 *   33-52     小猫 (20维)
 *   53-66     风景 (14维)
 *   67-127    预留
 */
public class TagVocabulary {

    public static final int VECTOR_SIZE = 128;

    // ==================== 大类 (0-2) ====================
    public static final TagDef CAT_BEAUTY   = new TagDef(0,  "美女");
    public static final TagDef CAT_CAT      = new TagDef(1,  "小猫");
    public static final TagDef CAT_SCENERY  = new TagDef(2,  "风景");

    // ==================== 穿搭-上装 (3-6) ====================
    public static final TagDef TOP_TANK     = new TagDef(3,  "吊带");
    public static final TagDef TOP_SHIRT    = new TagDef(4,  "衬衫");
    public static final TagDef TOP_TSHIRT   = new TagDef(5,  "T恤");
    public static final TagDef TOP_BLAZER   = new TagDef(6,  "西装");

    // ==================== 穿搭-下装 (7-11) ====================
    public static final TagDef BOT_SKIRT_S  = new TagDef(7,  "短裙");
    public static final TagDef BOT_SKIRT_L  = new TagDef(8,  "长裙");
    public static final TagDef BOT_JEANS    = new TagDef(9,  "牛仔裤");
    public static final TagDef BOT_YOGA     = new TagDef(10, "瑜伽裤");
    public static final TagDef BOT_PANTS    = new TagDef(11, "长裤");

    // ==================== 穿搭-套装 (12-15) ====================
    public static final TagDef SET_JK       = new TagDef(12, "JK制服");
    public static final TagDef SET_DRESS    = new TagDef(13, "连衣裙");
    public static final TagDef SET_QIPAO    = new TagDef(14, "旗袍");
    public static final TagDef SET_HANFU    = new TagDef(15, "汉服");

    // ==================== 丝袜 (18-21) ====================
    public static final TagDef SOCK_BLACK   = new TagDef(18, "黑丝");
    public static final TagDef SOCK_WHITE   = new TagDef(19, "白丝");
    public static final TagDef SOCK_FISHNET = new TagDef(20, "渔网");
    public static final TagDef SOCK_LONG    = new TagDef(21, "长袜");

    // ==================== 舞蹈 (22-23) ====================
    public static final TagDef DANCE_SOLO   = new TagDef(22, "单人舞");
    public static final TagDef DANCE_GROUP  = new TagDef(23, "多人舞");

    // ==================== 人物 (24-30) ====================
    public static final TagDef CHAR_YUJIE   = new TagDef(24, "御姐");
    public static final TagDef CHAR_TIANMEI = new TagDef(25, "甜妹");
    public static final TagDef HAIR_LONG    = new TagDef(26, "长发");
    public static final TagDef HAIR_SHORT   = new TagDef(27, "短发");
    public static final TagDef FEAT_GLASSES = new TagDef(28, "眼镜妹");
    public static final TagDef FEAT_SLIM    = new TagDef(29, "苗条");
    public static final TagDef FEAT_YOUNG   = new TagDef(30, "少女感");

    // ==================== 场景 (31-32) ====================
    public static final TagDef SCENE_IN     = new TagDef(31, "室内");
    public static final TagDef SCENE_OUT    = new TagDef(32, "室外");

    // ==================== 小猫-品种 (33-39) ====================
    public static final TagDef CAT_BREED_ORANGE   = new TagDef(33, "橘猫");
    public static final TagDef CAT_BREED_BRITISH  = new TagDef(34, "英短");
    public static final TagDef CAT_BREED_AMERICAN = new TagDef(35, "美短");
    public static final TagDef CAT_BREED_RAGDOLL  = new TagDef(36, "布偶猫");
    public static final TagDef CAT_BREED_SIAMESE  = new TagDef(37, "暹罗猫");
    public static final TagDef CAT_BREED_TIANYUAN = new TagDef(38, "田园猫");
    public static final TagDef CAT_BREED_OTHER    = new TagDef(39, "其他猫");

    // ==================== 小猫-颜色 (40-45) ====================
    public static final TagDef CAT_COLOR_ORANGE = new TagDef(40, "橘色");
    public static final TagDef CAT_COLOR_WHITE  = new TagDef(41, "白色");
    public static final TagDef CAT_COLOR_BLACK  = new TagDef(42, "黑色");
    public static final TagDef CAT_COLOR_GRAY   = new TagDef(43, "灰色");
    public static final TagDef CAT_COLOR_CALICO = new TagDef(44, "三花");
    public static final TagDef CAT_COLOR_TABBY  = new TagDef(45, "狸花");

    // ==================== 小猫-年龄 (46-47) ====================
    public static final TagDef CAT_AGE_KITTEN = new TagDef(46, "幼猫");
    public static final TagDef CAT_AGE_ADULT  = new TagDef(47, "成年猫");

    // ==================== 小猫-行为 (48-52) ====================
    public static final TagDef CAT_ACT_PLAY  = new TagDef(48, "玩耍");
    public static final TagDef CAT_ACT_SLEEP = new TagDef(49, "睡觉");
    public static final TagDef CAT_ACT_EAT   = new TagDef(50, "吃饭");
    public static final TagDef CAT_ACT_CUTE  = new TagDef(51, "卖萌");
    public static final TagDef CAT_ACT_GROOM = new TagDef(52, "舔毛");

    // ==================== 风景 (53-66) ====================
    public static final TagDef SCY_NATURE    = new TagDef(53, "自然风光");
    public static final TagDef SCY_CITYNIGHT = new TagDef(54, "城市夜景");
    public static final TagDef SCY_SEA       = new TagDef(55, "海景");
    public static final TagDef SCY_MOUNTAIN  = new TagDef(56, "山景");
    public static final TagDef SCY_SUNSET    = new TagDef(57, "日落");
    public static final TagDef SCY_SNOW      = new TagDef(58, "雪景");
    public static final TagDef SCY_FOREST    = new TagDef(59, "森林");
    public static final TagDef SCY_FLOWER    = new TagDef(60, "花海");
    public static final TagDef SCY_OLD_TOWN  = new TagDef(61, "古镇");
    public static final TagDef SCY_GRASSLAND = new TagDef(62, "草原");
    public static final TagDef SCY_LAKE      = new TagDef(63, "湖景");
    public static final TagDef SCY_WATERFALL = new TagDef(64, "瀑布");
    public static final TagDef SCY_DAY       = new TagDef(65, "白天");
    public static final TagDef SCY_NIGHT     = new TagDef(66, "夜晚");

    // ==================== ALL TAGS MAP ====================
    private static final Map<String, TagDef> TAG_MAP = new LinkedHashMap<>();
    private static final TagDef[] DIM_INDEX = new TagDef[VECTOR_SIZE];
    private static final Set<String> CATEGORY_TAGS = Set.of("美女", "小猫", "风景");

    static {
        for (java.lang.reflect.Field field : TagVocabulary.class.getDeclaredFields()) {
            if (field.getType() == TagDef.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    TagDef tag = (TagDef) field.get(null);
                    TAG_MAP.put(tag.label, tag);
                    if (tag.dim >= 0 && tag.dim < VECTOR_SIZE) {
                        DIM_INDEX[tag.dim] = tag;
                    }
                } catch (IllegalAccessException ignored) {}
            }
        }
    }

    public static TagDef find(String label) { return TAG_MAP.get(label); }

    public static String allTagsForPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("大类: 美女 小猫 风景\n");
        sb.append("上装: 吊带 衬衫 T恤 西装\n");
        sb.append("下装: 短裙 长裙 牛仔裤 瑜伽裤 长裤\n");
        sb.append("套装: JK制服 连衣裙 旗袍 汉服\n");
        sb.append("丝袜: 黑丝 白丝 渔网 长袜\n");
        sb.append("舞蹈: 单人舞 多人舞\n");
        sb.append("人物: 御姐 甜妹 长发 短发 眼镜妹 苗条 少女感\n");
        sb.append("场景: 室内 室外\n");
        sb.append("小猫: 橘猫 英短 美短 布偶猫 暹罗猫 田园猫 其他猫 橘色 白色 黑色 灰色 三花 狸花 幼猫 成年猫 玩耍 睡觉 吃饭 卖萌 舔毛\n");
        sb.append("风景: 自然风光 城市夜景 海景 山景 日落 雪景 森林 花海 古镇 草原 湖景 瀑布 白天 夜晚\n");
        return sb.toString();
    }

    public static float[] buildVector(List<String> tags) {
        float[] vec = new float[VECTOR_SIZE];
        for (String t : tags) {
            String trimmed = t.trim();
            TagDef def = TAG_MAP.get(trimmed);
            if (def != null && def.dim >= 0) vec[def.dim] = 1.0f;
        }
        float norm = 0f;
        for (float v : vec) norm += v * v;
        norm = (float) Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < vec.length; i++) vec[i] /= norm;
        return vec;
    }

    public static TagDef byDim(int dim) {
        return (dim >= 0 && dim < VECTOR_SIZE) ? DIM_INDEX[dim] : null;
    }

    public static boolean isCategoryTag(String label) {
        return CATEGORY_TAGS.contains(label);
    }

    public record TagDef(int dim, String label) {}
}
