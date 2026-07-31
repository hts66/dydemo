"""128维视频标签词汇表 — 与 Java TagVocabulary 保持同步

维度分配:
  0-9   内容大类 (美女/风景/美食/宠物/街拍/搞笑/教程/音乐/舞蹈/其他)
  10-49 美女属性 (类型/发型/发色/腿型/丝袜/眼镜/穿搭/身材/年龄感)
  50-69 风景属性
  70-127 预留
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
    # 内容大类 (0-9)
    TagDef(0, "美女"),
    TagDef(1, "风景"),
    TagDef(2, "美食"),
    TagDef(3, "宠物"),
    TagDef(4, "街拍"),
    TagDef(5, "搞笑"),
    TagDef(6, "教程"),
    TagDef(7, "音乐"),
    TagDef(8, "舞蹈"),
    TagDef(9, "其他"),

    # 美女 — 类型 (10-14)
    TagDef(10, "御姐"),
    TagDef(11, "萝莉"),
    TagDef(12, "甜妹"),
    TagDef(13, "酷飒"),
    TagDef(14, "清新"),

    # 美女 — 发型 (15-17)
    TagDef(15, "长发"),
    TagDef(16, "短发"),
    TagDef(17, "中长发"),

    # 美女 — 发色 (18-22)
    TagDef(18, "黑发"),
    TagDef(19, "棕发"),
    TagDef(20, "金发"),
    TagDef(21, "红发"),
    TagDef(22, "其他发色"),

    # 美女 — 腿型 (23-25)
    TagDef(23, "长腿"),
    TagDef(24, "肉腿"),
    TagDef(25, "普通腿型"),

    # 美女 — 丝袜 (26-31)
    TagDef(26, "光腿"),
    TagDef(27, "黑丝"),
    TagDef(28, "白丝"),
    TagDef(29, "肉丝"),
    TagDef(30, "渔网"),
    TagDef(31, "其他丝袜"),

    # 美女 — 眼镜 (32-33)
    TagDef(32, "眼镜妹"),
    TagDef(33, "无眼镜"),

    # 美女 — 穿搭 (34-43)
    TagDef(34, "连衣裙"),
    TagDef(35, "JK制服"),
    TagDef(36, "汉服"),
    TagDef(37, "旗袍"),
    TagDef(38, "吊带"),
    TagDef(39, "运动装"),
    TagDef(40, "休闲装"),
    TagDef(41, "性感穿搭"),
    TagDef(42, "可爱风"),
    TagDef(43, "职场装"),

    # 美女 — 身材 (44-46)
    TagDef(44, "苗条"),
    TagDef(45, "丰满"),
    TagDef(46, "骨感"),

    # 美女 — 年龄感 (47-49)
    TagDef(47, "少女感"),
    TagDef(48, "轻熟"),
    TagDef(49, "成熟"),

    # 风景 — 类型 (50-69)
    TagDef(50, "自然风光"),
    TagDef(51, "城市夜景"),
    TagDef(52, "海景"),
    TagDef(53, "山景"),
    TagDef(54, "日落"),
    TagDef(55, "雪景"),
    TagDef(56, "森林"),
    TagDef(57, "花海"),
    TagDef(58, "古镇"),
    TagDef(59, "街拍风景"),
    TagDef(60, "航拍"),
    TagDef(61, "慢动作"),
    TagDef(62, "延时摄影"),
    TagDef(63, "雨景"),
    TagDef(64, "云海"),
    TagDef(65, "极光"),
    TagDef(66, "沙漠"),
    TagDef(67, "草原"),
    TagDef(68, "湖景"),
    TagDef(69, "瀑布"),
]

# 构建查找字典
TAG_MAP: Dict[str, TagDef] = {t.label: t for t in ALL_TAGS}
DIM_INDEX: Dict[int, TagDef] = {t.dim: t for t in ALL_TAGS}


def find(label: str) -> Optional[TagDef]:
    """根据标签文本查找 TagDef"""
    return TAG_MAP.get(label)


def build_vector(tags: List[str]) -> List[float]:
    """从标签列表构建 128 维向量"""
    vec = [0.0] * VECTOR_SIZE
    for t in tags:
        td = TAG_MAP.get(t.strip())
        if td is not None:
            vec[td.dim] = 1.0
    return vec


def all_tags_for_prompt() -> str:
    """生成供 LLM prompt 使用的标签列表"""
    categories = [
        ("内容大类", range(0, 10)),
        ("美女-类型", range(10, 15)),
        ("美女-发型", range(15, 18)),
        ("美女-发色", range(18, 23)),
        ("美女-腿型", range(23, 26)),
        ("美女-丝袜", range(26, 32)),
        ("美女-眼镜", range(32, 34)),
        ("美女-穿搭", range(34, 44)),
        ("美女-身材", range(44, 47)),
        ("美女-年龄感", range(47, 50)),
        ("风景-类型", range(50, 70)),
    ]

    lines = []
    for name, r in categories:
        tag_labels = [DIM_INDEX[d].label for d in r if d in DIM_INDEX]
        lines.append(f"{name}: {' '.join(tag_labels)}")

    return "\n".join(lines)
