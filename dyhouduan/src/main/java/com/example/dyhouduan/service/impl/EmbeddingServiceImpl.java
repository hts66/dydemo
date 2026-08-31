package com.example.dyhouduan.service.impl;

import com.example.dyhouduan.config.EmbeddingConfig;
import com.example.dyhouduan.service.EmbeddingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通义 text-embedding-v3 实现（DashScope OpenAI 兼容 /embeddings 接口）。
 * 认证方式与 vision 一致：Authorization: Bearer sk-xxx。
 */
@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingConfig config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public EmbeddingServiceImpl(EmbeddingConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) return null;
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.error("embedding.api-key 未配置，无法调用向量化接口");
            return null;
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", config.getModel());
            body.put("input", text);
            body.put("dimensions", config.getDim());
            body.put("encoding_format", "float");

            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getApiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Embedding API 返回异常: {} {}", response.statusCode(), response.body());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.get("data");
            if (data != null && data.isArray() && !data.isEmpty()) {
                JsonNode embNode = data.get(0).get("embedding");
                if (embNode != null && embNode.isArray()) {
                    float[] vec = new float[embNode.size()];
                    for (int i = 0; i < embNode.size(); i++) {
                        vec[i] = (float) embNode.get(i).asDouble();
                    }
                    return vec;
                }
            }

            log.error("Embedding API 无有效返回: {}", response.body());
            return null;
        } catch (Exception e) {
            log.error("调用 Embedding API 失败", e);
            return null;
        }
    }
}
