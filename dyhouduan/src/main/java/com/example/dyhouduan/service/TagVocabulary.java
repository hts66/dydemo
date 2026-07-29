package com.example.dyhouduan.service;

import java.util.*;

/**
 * 视频标签词汇表 — 128维向量，每个维度对应一个标签
 *
 * 维度分配:
 *   0-9   内容大类 (美女/风景/美食/宠物/街拍/搞笑/教程/音乐/舞蹈/其他)
 *   10-49 美女属性 (类型/发型/发色/腿型/丝袜/眼镜/穿搭/身材/年龄感)
 *   50-69 风景属性 (自然/城市夜景/海景/山景/日落/雪景/森林/花海/古镇/街拍/航拍/慢动作/延时/雨景/云海/极光/沙漠/草原/湖景/瀑布)
 *   70-127 预留
 */
public class TagVocabulary {

    public static final int VECTOR_SIZE = 128;

    // ==================== 内容大类 (0-9) ====================
    public static final TagDef GENERAL_BEAUTY    = new TagDef(0,  "美女");
    public static final TagDef GENERAL_SCENERY   = new TagDef(1,  "风景");
    public static final TagDef GENERAL_FOOD      = new TagDef(2,  "美食");
    public static final TagDef GENERAL_PET       = new TagDef(3,  "宠物");
    public static final TagDef GENERAL_STREET    = new TagDef(4,  "街拍");
    public static final TagDef GENERAL_FUNNY     = new TagDef(5,  "搞笑");
    public static final TagDef GENERAL_TUTORIAL  = new TagDef(6,  "教程");
    public static final TagDef GENERAL_MUSIC     = new TagDef(7,  "音乐");
    public static final TagDef GENERAL_DANCE     = new TagDef(8,  "舞蹈");
    public static final TagDef GENERAL_OTHER     = new TagDef(9,  "其他");

    // ==================== 美女 — 类型 (10-14) ====================
    public static final TagDef BEAUTY_YUJIE   = new TagDef(10, "御姐");
    public static final TagDef BEAUTY_LUOLI   = new TagDef(11, "萝莉");
    public static final TagDef BEAUTY_TIANMEI = new TagDef(12, "甜妹");
    public static final TagDef BEAUTY_KUSA    = new TagDef(13, "酷飒");
    public static final TagDef BEAUTY_QINGXIN = new TagDef(14, "清新");

    // ==================== 美女 — 发型 (15-17) ====================
    public static final TagDef HAIR_LONG   = new TagDef(15, "长发");
    public static final TagDef HAIR_SHORT  = new TagDef(16, "短发");
    public static final TagDef HAIR_MEDIUM = new TagDef(17, "中长发");

    // ==================== 美女 — 发色 (18-22) ====================
    public static final TagDef HAIR_BLACK       = new TagDef(18, "黑发");
    public static final TagDef HAIR_BROWN       = new TagDef(19, "棕发");
    public static final TagDef HAIR_BLONDE      = new TagDef(20, "金发");
    public static final TagDef HAIR_RED         = new TagDef(21, "红发");
    public static final TagDef HAIR_OTHER_COLOR = new TagDef(22, "其他发色");

    // ==================== 美女 — 腿型 (23-25) ====================
    public static final TagDef LEGS_LONG   = new TagDef(23, "长腿");
    public static final TagDef LEGS_THICK  = new TagDef(24, "肉腿");
    public static final TagDef LEGS_NORMAL = new TagDef(25, "普通腿型");

    // ==================== 美女 — 丝袜 (26-31) ====================
    public static final TagDef STOCKINGS_BARE     = new TagDef(26, "光腿");
    public static final TagDef STOCKINGS_BLACK    = new TagDef(27, "黑丝");
    public static final TagDef STOCKINGS_WHITE    = new TagDef(28, "白丝");
    public static final TagDef STOCKINGS_NUDE     = new TagDef(29, "肉丝");
    public static final TagDef STOCKINGS_FISHNET  = new TagDef(30, "渔网");
    public static final TagDef STOCKINGS_OTHER    = new TagDef(31, "其他丝袜");

    // ==================== 美女 — 眼镜 (32-33) ====================
    public static final TagDef GLASSES_YES = new TagDef(32, "眼镜妹");
    public static final TagDef GLASSES_NO  = new TagDef(33, "无眼镜");

    // ==================== 美女 — 穿搭 (34-43) ====================
    public static final TagDef OUTFIT_DRESS   = new TagDef(34, "连衣裙");
    public static final TagDef OUTFIT_JK      = new TagDef(35, "JK制服");
    public static final TagDef OUTFIT_HANFU   = new TagDef(36, "汉服");
    public static final TagDef OUTFIT_QIPAO   = new TagDef(37, "旗袍");
    public static final TagDef OUTFIT_TANK    = new TagDef(38, "吊带");
    public static final TagDef OUTFIT_SPORT   = new TagDef(39, "运动装");
    public static final TagDef OUTFIT_CASUAL  = new TagDef(40, "休闲装");
    public static final TagDef OUTFIT_SEXY    = new TagDef(41, "性感穿搭");
    public static final TagDef OUTFIT_CUTE    = new TagDef(42, "可爱风");
    public static final TagDef OUTFIT_OFFICE  = new TagDef(43, "职场装");

    // ==================== 美女 — 身材 (44-46) ====================
    public static final TagDef BODY_SLIM  = new TagDef(44, "苗条");
    public static final TagDef BODY_PLUMP = new TagDef(45, "丰满");
    public static final TagDef BODY_THIN  = new TagDef(46, "骨感");

    // ==================== 美女 — 年龄感 (47-49) ====================
    public static final TagDef AGE_YOUNG        = new TagDef(47, "少女感");
    public static final TagDef AGE_LIGHT_MATURE = new TagDef(48, "轻熟");
    public static final TagDef AGE_MATURE       = new TagDef(49, "成熟");

    // ==================== 风景 — 类型 (50-69) ====================
    public static final TagDef SCENERY_NATURE     = new TagDef(50, "自然风光");
    public static final TagDef SCENERY_CITY_NIGHT = new TagDef(51, "城市夜景");
    public static final TagDef SCENERY_SEA        = new TagDef(52, "海景");
    public static final TagDef SCENERY_MOUNTAIN   = new TagDef(53, "山景");
    public static final TagDef SCENERY_SUNSET     = new TagDef(54, "日落");
    public static final TagDef SCENERY_SNOW       = new TagDef(55, "雪景");
    public static final TagDef SCENERY_FOREST     = new TagDef(56, "森林");
    public static final TagDef SCENERY_FLOWER     = new TagDef(57, "花海");
    public static final TagDef SCENERY_OLD_TOWN   = new TagDef(58, "古镇");
    public static final TagDef SCENERY_STREET     = new TagDef(59, "街拍风景");
    public static final TagDef SCENERY_AERIAL     = new TagDef(60, "航拍");
    public static final TagDef SCENERY_SLOW_MO    = new TagDef(61, "慢动作");
    public static final TagDef SCENERY_TIMELAPSE  = new TagDef(62, "延时摄影");
    public static final TagDef SCENERY_RAIN       = new TagDef(63, "雨景");
    public static final TagDef SCENERY_CLOUD      = new TagDef(64, "云海");
    public static final TagDef SCENERY_AURORA     = new TagDef(65, "极光");
    public static final TagDef SCENERY_DESERT     = new TagDef(66, "沙漠");
    public static final TagDef SCENERY_GRASSLAND  = new TagDef(67, "草原");
    public static final TagDef SCENERY_LAKE       = new TagDef(68, "湖景");
    public static final TagDef SCENERY_WATERFALL  = new TagDef(69, "瀑布");

    // ==================== ALL TAGS MAP ====================
    private static final Map<String, TagDef> TAG_MAP = new LinkedHashMap<>();
    private static final TagDef[] DIM_INDEX = new TagDef[VECTOR_SIZE];

    static {
        for (java.lang.reflect.Field field : TagVocabulary.class.getDeclaredFields()) {
            if (field.getType() == TagDef.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    TagDef tag = (TagDef) field.get(null);
                    TAG_MAP.put(tag.label, tag);
                    DIM_INDEX[tag.dim] = tag;
                } catch (IllegalAccessException ignored) {}
            }
        }
    }

    /** 根据标签文本查找 TagDef */
    public static TagDef find(String label) {
        return TAG_MAP.get(label);
    }

    /** 列出所有标签（供 DeepSeek prompt 使用） */
    public static String allTagsForPrompt() {
        StringBuilder sb = new StringBuilder();
        String[] categories = {
            "内容大类:", "美女-类型:", "美女-发型:", "美女-发色:",
            "美女-腿型:", "美女-丝袜:", "美女-眼镜:", "美女-穿搭:",
            "美女-身材:", "美女-年龄感:", "风景-类型:"
        };
        int[][] ranges = {
            {0,9}, {10,14}, {15,17}, {18,22},
            {23,25}, {26,31}, {32,33}, {34,43},
            {44,46}, {47,49}, {50,69}
        };
        for (int i = 0; i < categories.length; i++) {
            sb.append(categories[i]);
            for (int d = ranges[i][0]; d <= ranges[i][1]; d++) {
                if (DIM_INDEX[d] != null) {
                    sb.append(" ").append(DIM_INDEX[d].label);
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /** 从标签列表构建 128 维向量 */
    public static float[] buildVector(List<String> tags) {
        float[] vec = new float[VECTOR_SIZE];
        for (String t : tags) {
            TagDef def = TAG_MAP.get(t.trim());
            if (def != null) {
                vec[def.dim] = 1.0f;
            }
        }
        return vec;
    }

    /** 标签定义 */
    public record TagDef(int dim, String label) {}
}
