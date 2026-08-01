package com.example.dyhouduan.service;

import java.util.*;

/**
 * 视频标签词汇表 — 128维向量，每维对应一个标签
 *
 * 维度分配 (one-hot类别编码):
 *   0        美女大类
 *   1        小猫大类
 *   2        风景大类
 *   3-42     美女属性 (类型/发型/发色/穿搭/身材/年龄感/动作)
 *   43-66    小猫属性 (品种/颜色/年龄/行为/数量/场景)
 *   67-88    风景属性 (场景类型/拍摄手法/时间)
 *   89-127   预留
 *
 * 规则: 画面内容与子类标签不符合时只标记大类，子类全部为0
 */
public class TagVocabulary {

    public static final int VECTOR_SIZE = 128;

    // ==================== 大类 (one-hot: dim 0-2) ====================
    public static final TagDef CATEGORY_BEAUTY  = new TagDef(0, "美女");
    public static final TagDef CATEGORY_CAT     = new TagDef(1, "小猫");
    public static final TagDef CATEGORY_SCENERY = new TagDef(2, "风景");

    // ==================== 美女 — 类型 (3-7) ====================
    public static final TagDef BEAUTY_YUJIE   = new TagDef(3, "御姐");
    public static final TagDef BEAUTY_LUOLI   = new TagDef(4, "萝莉");
    public static final TagDef BEAUTY_TIANMEI = new TagDef(5, "甜妹");
    public static final TagDef BEAUTY_KUSA    = new TagDef(6, "酷飒");
    public static final TagDef BEAUTY_QINGXIN = new TagDef(7, "清新");

    // ==================== 美女 — 发型 (8-10) ====================
    public static final TagDef HAIR_LONG   = new TagDef(8, "长发");
    public static final TagDef HAIR_SHORT  = new TagDef(9, "短发");
    public static final TagDef HAIR_MEDIUM = new TagDef(10, "中长发");

    // ==================== 美女 — 发色 (11-14) ====================
    public static final TagDef HAIR_BLACK       = new TagDef(11, "黑发");
    public static final TagDef HAIR_BROWN       = new TagDef(12, "棕发");
    public static final TagDef HAIR_BLONDE      = new TagDef(13, "金发");
    public static final TagDef HAIR_OTHER_COLOR = new TagDef(14, "其他发色");

    // ==================== 美女 — 腿型 (15-17) ====================
    public static final TagDef LEGS_LONG   = new TagDef(15, "长腿");
    public static final TagDef LEGS_THICK  = new TagDef(16, "肉腿");
    public static final TagDef LEGS_NORMAL = new TagDef(17, "普通腿型");

    // ==================== 美女 — 丝袜 (18-23) ====================
    public static final TagDef STOCKINGS_BARE     = new TagDef(18, "光腿");
    public static final TagDef STOCKINGS_BLACK    = new TagDef(19, "黑丝");
    public static final TagDef STOCKINGS_WHITE    = new TagDef(20, "白丝");
    public static final TagDef STOCKINGS_NUDE     = new TagDef(21, "肉丝");
    public static final TagDef STOCKINGS_FISHNET  = new TagDef(22, "渔网");
    public static final TagDef STOCKINGS_OTHER    = new TagDef(23, "其他丝袜");

    // ==================== 美女 — 眼镜 (24-25) ====================
    public static final TagDef GLASSES_YES = new TagDef(24, "眼镜妹");
    public static final TagDef GLASSES_NO  = new TagDef(25, "无眼镜");

    // ==================== 美女 — 穿搭 (26-35) ====================
    public static final TagDef OUTFIT_DRESS   = new TagDef(26, "连衣裙");
    public static final TagDef OUTFIT_JK      = new TagDef(27, "JK制服");
    public static final TagDef OUTFIT_HANFU   = new TagDef(28, "汉服");
    public static final TagDef OUTFIT_QIPAO   = new TagDef(29, "旗袍");
    public static final TagDef OUTFIT_TANK    = new TagDef(30, "吊带");
    public static final TagDef OUTFIT_SPORT   = new TagDef(31, "运动装");
    public static final TagDef OUTFIT_CASUAL  = new TagDef(32, "休闲装");
    public static final TagDef OUTFIT_SEXY    = new TagDef(33, "性感穿搭");
    public static final TagDef OUTFIT_CUTE    = new TagDef(34, "可爱风");
    public static final TagDef OUTFIT_OFFICE  = new TagDef(35, "职场装");

    // ==================== 美女 — 身材 (36-38) ====================
    public static final TagDef BODY_SLIM  = new TagDef(36, "苗条");
    public static final TagDef BODY_PLUMP = new TagDef(37, "丰满");
    public static final TagDef BODY_THIN  = new TagDef(38, "骨感");

    // ==================== 美女 — 年龄感 (39-41) ====================
    public static final TagDef AGE_YOUNG        = new TagDef(39, "少女感");
    public static final TagDef AGE_LIGHT_MATURE = new TagDef(40, "轻熟");
    public static final TagDef AGE_MATURE       = new TagDef(41, "成熟");

    // ==================== 美女 — 动作 (42) ====================
    public static final TagDef ACTION_DANCE = new TagDef(42, "跳舞");

    // ==================== 小猫 — 品种 (43-49) ====================
    public static final TagDef CAT_BREED_ORANGE  = new TagDef(43, "橘猫");
    public static final TagDef CAT_BREED_BRITISH = new TagDef(44, "英短");
    public static final TagDef CAT_BREED_AMERICAN = new TagDef(45, "美短");
    public static final TagDef CAT_BREED_RAGDOLL = new TagDef(46, "布偶猫");
    public static final TagDef CAT_BREED_SIAMESE = new TagDef(47, "暹罗猫");
    public static final TagDef CAT_BREED_TIANYUAN = new TagDef(48, "田园猫");
    public static final TagDef CAT_BREED_OTHER   = new TagDef(49, "其他猫");

    // ==================== 小猫 — 颜色 (50-55) ====================
    public static final TagDef CAT_COLOR_ORANGE   = new TagDef(50, "橘色");
    public static final TagDef CAT_COLOR_WHITE    = new TagDef(51, "白色");
    public static final TagDef CAT_COLOR_BLACK    = new TagDef(52, "黑色");
    public static final TagDef CAT_COLOR_GRAY     = new TagDef(53, "灰色");
    public static final TagDef CAT_COLOR_CALICO   = new TagDef(54, "三花");
    public static final TagDef CAT_COLOR_TABBY    = new TagDef(55, "狸花");

    // ==================== 小猫 — 年龄 (56-57) ====================
    public static final TagDef CAT_AGE_KITTEN  = new TagDef(56, "幼猫");
    public static final TagDef CAT_AGE_ADULT   = new TagDef(57, "成年猫");

    // ==================== 小猫 — 行为 (58-62) ====================
    public static final TagDef CAT_ACT_PLAY     = new TagDef(58, "玩耍");
    public static final TagDef CAT_ACT_SLEEP    = new TagDef(59, "睡觉");
    public static final TagDef CAT_ACT_EAT      = new TagDef(60, "吃饭");
    public static final TagDef CAT_ACT_CUTE     = new TagDef(61, "卖萌");
    public static final TagDef CAT_ACT_GROOM    = new TagDef(62, "舔毛");

    // ==================== 小猫 — 数量 (63-64) ====================
    public static final TagDef CAT_COUNT_SINGLE = new TagDef(63, "单只");
    public static final TagDef CAT_COUNT_MULTI  = new TagDef(64, "多只");

    // ==================== 小猫 — 场景 (65-66) ====================
    public static final TagDef CAT_SCENE_INDOOR  = new TagDef(65, "室内");
    public static final TagDef CAT_SCENE_OUTDOOR = new TagDef(66, "室外");

    // ==================== 风景 — 场景类型 (67-82) ====================
    public static final TagDef SCENERY_NATURE     = new TagDef(67, "自然风光");
    public static final TagDef SCENERY_CITY_NIGHT = new TagDef(68, "城市夜景");
    public static final TagDef SCENERY_SEA        = new TagDef(69, "海景");
    public static final TagDef SCENERY_MOUNTAIN   = new TagDef(70, "山景");
    public static final TagDef SCENERY_SUNSET     = new TagDef(71, "日落");
    public static final TagDef SCENERY_SNOW       = new TagDef(72, "雪景");
    public static final TagDef SCENERY_FOREST     = new TagDef(73, "森林");
    public static final TagDef SCENERY_FLOWER     = new TagDef(74, "花海");
    public static final TagDef SCENERY_OLD_TOWN   = new TagDef(75, "古镇");
    public static final TagDef SCENERY_GRASSLAND  = new TagDef(76, "草原");
    public static final TagDef SCENERY_LAKE       = new TagDef(77, "湖景");
    public static final TagDef SCENERY_WATERFALL  = new TagDef(78, "瀑布");
    public static final TagDef SCENERY_DESERT     = new TagDef(79, "沙漠");
    public static final TagDef SCENERY_AURORA     = new TagDef(80, "极光");
    public static final TagDef SCENERY_CLOUD      = new TagDef(81, "云海");
    public static final TagDef SCENERY_RAIN       = new TagDef(82, "雨景");

    // ==================== 风景 — 拍摄手法 (83-86) ====================
    public static final TagDef TECH_AERIAL     = new TagDef(83, "航拍");
    public static final TagDef TECH_TIMELAPSE  = new TagDef(84, "延时");
    public static final TagDef TECH_SLOW_MO    = new TagDef(85, "慢动作");
    public static final TagDef TECH_NORMAL     = new TagDef(86, "常规拍摄");

    // ==================== 风景 — 时间 (87-88) ====================
    public static final TagDef TIME_DAY   = new TagDef(87, "白天");
    public static final TagDef TIME_NIGHT = new TagDef(88, "夜晚");

    // ==================== ALL TAGS MAP ====================
    private static final Map<String, TagDef> TAG_MAP = new LinkedHashMap<>();
    private static final TagDef[] DIM_INDEX = new TagDef[VECTOR_SIZE];

    // 大类标签
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

    /** 根据标签文本查找 TagDef */
    public static TagDef find(String label) {
        return TAG_MAP.get(label);
    }

    /** 列出所有标签（供 LLM prompt 使用） */
    public static String allTagsForPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("大类: 美女 小猫 风景\n");

        String[] categories = {
            "美女-类型:", "美女-发型:", "美女-发色:",
            "美女-腿型:", "美女-丝袜:", "美女-眼镜:", "美女-穿搭:",
            "美女-身材:", "美女-年龄感:", "美女-动作:",
            "小猫-品种:", "小猫-颜色:", "小猫-年龄:", "小猫-行为:",
            "小猫-数量:", "小猫-场景:",
            "风景-类型:", "风景-拍摄:", "风景-时间:"
        };
        int[][] ranges = {
            {3,7}, {8,10}, {11,14},
            {15,17}, {18,23}, {24,25}, {26,35},
            {36,38}, {39,41}, {42,42},
            {43,49}, {50,55}, {56,57}, {58,62},
            {63,64}, {65,66},
            {67,82}, {83,86}, {87,88}
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

    /** 从标签列表构建 128 维向量（one-hot 类别编码） */
    public static float[] buildVector(List<String> tags) {
        float[] vec = new float[VECTOR_SIZE];
        for (String t : tags) {
            String trimmed = t.trim();
            TagDef def = TAG_MAP.get(trimmed);
            if (def != null && def.dim >= 0) {
                vec[def.dim] = 1.0f;
            }
        }
        return vec;
    }

    /** 根据维度索引查找 TagDef */
    public static TagDef byDim(int dim) {
        if (dim >= 0 && dim < VECTOR_SIZE) {
            return DIM_INDEX[dim];
        }
        return null;
    }

    /** 判断是否为大类标签 */
    public static boolean isCategoryTag(String label) {
        return CATEGORY_TAGS.contains(label);
    }

    /** 标签定义 */
    public record TagDef(int dim, String label) {}
}
