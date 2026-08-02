"""128维视频标签词汇表 — 精准度优先，简化版

维度分配 (one-hot类别编码):
  0-2       大类 (美女/小猫/风景)
  3-17      穿搭 (15维)
  18-21     丝袜 (4维)
  22-23     舞蹈 (2维)
  24-30     人物 (7维)
  31-32     场景 (2维)
  33-52     小猫 (20维)
  53-66     风景 (14维)
  67-127    预留
"""

from typing import List, Dict, Optional
from dataclasses import dataclass

VECTOR_SIZE = 128


@dataclass
class TagDef:
    dim: int
    label: str


ALL_TAGS: List[TagDef] = [
    # ==================== 大类 (0-2) ====================
    TagDef(0, "美女"),
    TagDef(1, "小猫"),
    TagDef(2, "风景"),

    # ==================== 穿搭 (3-17) ====================
    # 上装 (3-6)
    TagDef(3, "吊带"),
    TagDef(4, "衬衫"),
    TagDef(5, "T恤"),
    TagDef(6, "西装"),
    # 下装 (7-11)
    TagDef(7, "短裙"),
    TagDef(8, "长裙"),
    TagDef(9, "牛仔裤"),
    TagDef(10, "瑜伽裤"),
    TagDef(11, "长裤"),
    # 套装 (12-17)
    TagDef(12, "JK制服"),
    TagDef(13, "连衣裙"),
    TagDef(14, "旗袍"),
    TagDef(15, "汉服"),

    # ==================== 丝袜 (16-21) → actually 18-21 ====================
    TagDef(18, "黑丝"),
    TagDef(19, "白丝"),
    TagDef(20, "渔网"),
    TagDef(21, "长袜"),

    # ==================== 舞蹈 (22-23) ====================
    TagDef(22, "单人舞"),
    TagDef(23, "多人舞"),

    # ==================== 人物 (24-30) ====================
    TagDef(24, "御姐"),
    TagDef(25, "甜妹"),
    TagDef(26, "长发"),
    TagDef(27, "短发"),
    TagDef(28, "眼镜妹"),
    TagDef(29, "苗条"),
    TagDef(30, "少女感"),

    # ==================== 场景 (31-32) ====================
    TagDef(31, "室内"),
    TagDef(32, "室外"),

    # ==================== 小猫-品种 (33-39) ====================
    TagDef(33, "橘猫"),
    TagDef(34, "英短"),
    TagDef(35, "美短"),
    TagDef(36, "布偶猫"),
    TagDef(37, "暹罗猫"),
    TagDef(38, "田园猫"),
    TagDef(39, "其他猫"),

    # ==================== 小猫-颜色 (40-45) ====================
    TagDef(40, "橘色"),
    TagDef(41, "白色"),
    TagDef(42, "黑色"),
    TagDef(43, "灰色"),
    TagDef(44, "三花"),
    TagDef(45, "狸花"),

    # ==================== 小猫-年龄 (46-47) ====================
    TagDef(46, "幼猫"),
    TagDef(47, "成年猫"),

    # ==================== 小猫-行为 (48-52) ====================
    TagDef(48, "玩耍"),
    TagDef(49, "睡觉"),
    TagDef(50, "吃饭"),
    TagDef(51, "卖萌"),
    TagDef(52, "舔毛"),

    # ==================== 风景 (53-66) ====================
    TagDef(53, "自然风光"),
    TagDef(54, "城市夜景"),
    TagDef(55, "海景"),
    TagDef(56, "山景"),
    TagDef(57, "日落"),
    TagDef(58, "雪景"),
    TagDef(59, "森林"),
    TagDef(60, "花海"),
    TagDef(61, "古镇"),
    TagDef(62, "草原"),
    TagDef(63, "湖景"),
    TagDef(64, "瀑布"),
    TagDef(65, "白天"),
    TagDef(66, "夜晚"),
]

TAG_MAP: Dict[str, TagDef] = {t.label: t for t in ALL_TAGS}
DIM_INDEX: Dict[int, TagDef] = {t.dim: t for t in ALL_TAGS if t.dim >= 0}


def find(label: str) -> Optional[TagDef]:
    return TAG_MAP.get(label)


def build_vector(tags: List[str]) -> List[float]:
    vec = [0.0] * VECTOR_SIZE
    for t in tags:
        trimmed = t.strip()
        td = TAG_MAP.get(trimmed)
        if td is not None and td.dim >= 0:
            vec[td.dim] = 1.0
    norm = sum(v * v for v in vec) ** 0.5
    if norm > 0:
        vec = [v / norm for v in vec]
    return vec


def all_tags_for_prompt() -> str:
    lines = [
        "大类: 美女 小猫 风景",
        "上装: 吊带 衬衫 T恤 西装",
        "下装: 短裙 长裙 牛仔裤 瑜伽裤 长裤",
        "套装: JK制服 连衣裙 旗袍 汉服",
        "丝袜: 黑丝 白丝 渔网 长袜",
        "舞蹈: 单人舞 多人舞",
        "人物: 御姐 甜妹 长发 短发 眼镜妹 苗条 少女感",
        "场景: 室内 室外",
        "小猫: 橘猫 英短 美短 布偶猫 暹罗猫 田园猫 其他猫 橘色 白色 黑色 灰色 三花 狸花 幼猫 成年猫 玩耍 睡觉 吃饭 卖萌 舔毛",
        "风景: 自然风光 城市夜景 海景 山景 日落 雪景 森林 花海 古镇 草原 湖景 瀑布 白天 夜晚",
    ]
    return "\n".join(lines)
