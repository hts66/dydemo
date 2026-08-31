package com.example.dyhouduan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文本向量(embedding)模型配置 — 通义 text-embedding-v3
 * 默认复用 vision 的 DashScope key（同一个百炼账号），只需在 application.properties 填一次。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingConfig {
    private String apiKey = "";
    private String apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";
    private String model = "text-embedding-v3";
    private int dim = 1024;
}
