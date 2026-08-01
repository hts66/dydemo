"""128维视频标签词汇表 — 与 Java TagVocabulary 保持同步

维度分配 (one-hot类别编码):
  0        美女大类
  1        小猫大类
  2        风景大类
  3-42     美女属性
  43-66    小猫属性
  67-88    风景属性
  89-127   预留
"""

from typing import List, Dict, Optional
from dataclasses import dataclass

VECTOR_SIZE = 128


@dataclass
class TagDef:
    dim: int
    label: str


# ==================== 所有标签定义 ====================

ALL_TAGS: List[TagDef] = [
    # ==================== 大类 (one-hot: dim 0-2) ====================
    TagDef(0, "美女"),
    TagDef(1, "小猫"),
    TagDef(2, "风景"),

    # ==================== 美女 — 类型 (3-7) ====================
    TagDef(3, "御姐"),
    TagDef(4, "萝莉"),
    TagDef(5, "甜妹"),
    TagDef(6, "酷飒"),
    TagDef(7, "清新"),

    # ==================== 美女 — 发型 (8-10) ====================
    TagDef(8, "长发"),
    TagDef(9, "短发"),
    TagDef(10, "中长发"),

    # ==================== 美女 — 发色 (11-14) ====================
    TagDef(11, "黑发"),
    TagDef(12, "棕发"),
    TagDef(13, "金发"),
    TagDef(14, "其他发色"),

    # ==================== 美女 — 腿型 (15-17) ====================
    TagDef(15, "长腿"),
    TagDef(16, "肉腿"),
    TagDef(17, "普通腿型"),

    # ==================== 美女 — 丝袜 (18-23) ====================
    TagDef(18, "光腿"),
    TagDef(19, "黑丝"),
    TagDef(20, "白丝"),
    TagDef(21, "肉丝"),
    TagDef(22, "渔网"),
    TagDef(23, "其他丝袜"),

    # ==================== 美女 — 眼镜 (24-25) ====================
    TagDef(24, "眼镜妹"),
    TagDef(25, "无眼镜"),

    # ==================== 美女 — 穿搭 (26-35) ====================
    TagDef(26, "连衣裙"),
    TagDef(27, "JK制服"),
    TagDef(28, "汉服"),
    TagDef(29, "旗袍"),
    TagDef(30, "吊带"),
    TagDef(31, "运动装"),
    TagDef(32, "休闲装"),
    TagDef(33, "性感穿搭"),
    TagDef(34, "可爱风"),
    TagDef(35, "职场装"),

    # ==================== 美女 — 身材 (36-38) ====================
    TagDef(36, "苗条"),
    TagDef(37, "丰满"),
    TagDef(38, "骨感"),

    # ==================== 美女 — 年龄感 (39-41) ====================
    TagDef(39, "少女感"),
    TagDef(40, "轻熟"),
    TagDef(41, "成熟"),

    # ==================== 美女 — 动作 (42) ====================
    TagDef(42, "跳舞"),

    # ==================== 小猫 — 品种 (43-49) ====================
    TagDef(43, "橘猫"),
    TagDef(44, "英短"),
    TagDef(45, "美短"),
    TagDef(46, "布偶猫"),
    TagDef(47, "暹罗猫"),
    TagDef(48, "田园猫"),
    TagDef(49, "其他猫"),

    # ==================== 小猫 — 颜色 (50-55) ====================
    TagDef(50, "橘色"),
    TagDef(51, "白色"),
    TagDef(52, "黑色"),
    TagDef(53, "灰色"),
    TagDef(54, "三花"),
    TagDef(55, "狸花"),

    # ==================== 小猫 — 年龄 (56-57) ====================
    TagDef(56, "幼猫"),
    TagDef(57, "成年猫"),

    # ==================== 小猫 — 行为 (58-62) ====================
    TagDef(58, "玩耍"),
    TagDef(59, "睡觉"),
    TagDef(60, "吃饭"),
    TagDef(61, "卖萌"),
    TagDef(62, "舔毛"),

    # ==================== 小猫 — 数量 (63-64) ====================
    TagDef(63, "单只"),
    TagDef(64, "多只"),

    # ==================== 小猫 — 场景 (65-66) ====================
    TagDef(65, "室内"),
    TagDef(66, "室外"),

    # ==================== 风景 — 场景类型 (67-82) ====================
    TagDef(67, "自然风光"),
    TagDef(68, "城市夜景"),
    TagDef(69, "海景"),
    TagDef(70, "山景"),
    TagDef(71, "日落"),
    TagDef(72, "雪景"),
    TagDef(73, "森林"),
    TagDef(74, "花海"),
    TagDef(75, "古镇"),
    TagDef(76, "草原"),
    TagDef(77, "湖景"),
    TagDef(78, "瀑布"),
    TagDef(79, "沙漠"),
    TagDef(80, "极光"),
    TagDef(81, "云海"),
    TagDef(82, "雨景"),

    # ==================== 风景 — 拍摄手法 (83-86) ====================
    TagDef(83, "航拍"),
    TagDef(84, "延时"),
    TagDef(85, "慢动作"),
    TagDef(86, "常规拍摄"),

    # ==================== 风景 — 时间 (87-88) ====================
    TagDef(87, "白天"),
    TagDef(88, "夜晚"),
]

# 构建查找字典
TAG_MAP: Dict[str, TagDef] = {t.label: t for t in ALL_TAGS}
DIM_INDEX: Dict[int, TagDef] = {t.dim: t for t in ALL_TAGS if t.dim >= 0}


def find(label: str) -> Optional[TagDef]:
    """根据标签文本查找 TagDef"""
    return TAG_MAP.get(label)


def build_vector(tags: List[str]) -> List[float]:
    """从标签列表构建 128 维向量（one-hot 类别编码）"""
    vec = [0.0] * VECTOR_SIZE
    for t in tags:
        trimmed = t.strip()
        td = TAG_MAP.get(trimmed)
        if td is not None and td.dim >= 0:
            vec[td.dim] = 1.0
    return vec


def all_tags_for_prompt() -> str:
    """生成供 LLM prompt 使用的标签列表"""
    categories = [
        ("大类(必选1个)", ["美女", "小猫", "风景"]),
        ("美女-类型", range(3, 8)),
        ("美女-发型", range(8, 11)),
        ("美女-发色", range(11, 15)),
        ("美女-腿型", range(15, 18)),
        ("美女-丝袜", range(18, 24)),
        ("美女-眼镜", range(24, 26)),
        ("美女-穿搭", range(26, 36)),
        ("美女-身材", range(36, 39)),
        ("美女-年龄感", range(39, 42)),
        ("美女-动作", range(42, 43)),
        ("小猫-品种", range(43, 50)),
        ("小猫-颜色", range(50, 56)),
        ("小猫-年龄", range(56, 58)),
        ("小猫-行为", range(58, 63)),
        ("小猫-数量", range(63, 65)),
        ("小猫-场景", range(65, 67)),
        ("风景-类型", range(67, 83)),
        ("风景-拍摄", range(83, 87)),
        ("风景-时间", range(87, 89)),
    ]

    lines = []
    for name, r in categories:
        if isinstance(r, list):
            lines.append(f"{name} {' '.join(r)}")
        else:
            tag_labels = [DIM_INDEX[d].label for d in r if d in DIM_INDEX]
            lines.append(f"{name}: {' '.join(tag_labels)}")

    return "\n".join(lines)
