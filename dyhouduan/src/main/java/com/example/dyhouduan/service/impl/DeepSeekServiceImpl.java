package com.example.dyhouduan.service.impl;

import com.example.dyhouduan.config.DeepSeekConfig;
import com.example.dyhouduan.service.DeepSeekService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class DeepSeekServiceImpl implements DeepSeekService {

    private final DeepSeekConfig deepSeekConfig;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // 视觉模型配置（通义千问 VL）
    @Value("${vision.api-key}")
    private String visionApiKey;

    @Value("${vision.api-url}")
    private String visionApiUrl;

    @Value("${vision.model}")
    private String visionModel;

    public DeepSeekServiceImpl(DeepSeekConfig deepSeekConfig) {
        this.deepSeekConfig = deepSeekConfig;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Override
    public String chat(String message) {
        return callApi(List.of(
                Map.of("role", "user", "content", message)
        ));
    }

    @Override
    public String chatWithHistory(List<Map<String, String>> messages) {
        List<Map<String, String>> full = new ArrayList<>();
        full.add(Map.of("role", "system", "content",
            "你是短视频平台AI助手\"小助\"，活泼可爱、热情友好。回复简洁自然，像朋友聊天，2-4句话即可。"));
        full.addAll(messages);
        return callApi(full);
    }

    @Override
    public String chatVision(String prompt, List<String> base64Images) {
        // 构建 multimodal content 数组
        List<Map<String, Object>> contentParts = new ArrayList<>();

        // 文本部分
        contentParts.add(Map.of("type", "text", "text", prompt));

        // 图片部分
        for (String b64 : base64Images) {
            Map<String, Object> imgPart = new LinkedHashMap<>();
            imgPart.put("type", "image_url");
            imgPart.put("image_url", Map.of("url", "data:image/jpeg;base64," + b64));
            contentParts.add(imgPart);
        }

        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", contentParts);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", visionModel);
            requestBody.put("messages", List.of(userMsg));
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 2048);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(visionApiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + visionApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(90))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode messageNode = choices.get(0).get("message");
                if (messageNode != null && messageNode.has("content")) {
                    return messageNode.get("content").asText();
                }
            }

            log.error("视觉API返回异常: {}", response.body());
            return "[]";
        } catch (Exception e) {
            log.error("调用视觉API失败", e);
            return "[]";
        }
    }

    /** 标准文本 API 调用（DeepSeek） */
    private String callApi(List<Map<String, String>> messages) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", deepSeekConfig.getModel());
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2048);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(deepSeekConfig.getApiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(90))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode messageNode = choices.get(0).get("message");
                if (messageNode != null && messageNode.has("content")) {
                    return messageNode.get("content").asText();
                }
            }

            log.error("DeepSeek API 返回异常: {}", response.body());
            return "抱歉，我暂时无法回答你的问题。";
        } catch (Exception e) {
            log.error("调用DeepSeek API失败", e);
            return "抱歉，我暂时无法回答你的问题。";
        }
    }
}
