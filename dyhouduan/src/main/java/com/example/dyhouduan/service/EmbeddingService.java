package com.example.dyhouduan.service;

/**
 * 文本向量化服务 — 把一段文本编码为语义 embedding 向量
 */
public interface EmbeddingService {

    /**
     * 将文本编码为向量。
     * @param text 待编码文本（视频侧为“标题+描述+画面描述+标签”，query 侧为用户原始查询）
     * @return 向量数组；调用失败或文本为空时返回 null
     */
    float[] embed(String text);
}
