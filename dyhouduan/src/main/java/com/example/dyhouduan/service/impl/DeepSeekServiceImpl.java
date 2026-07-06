package com.example.dyhouduan.service.impl;

import com.example.dyhouduan.config.DeepSeekConfig;
import com.example.dyhouduan.service.DeepSeekService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DeepSeekServiceImpl implements DeepSeekService {

    private final DeepSeekConfig deepSeekConfig;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DeepSeekServiceImpl(DeepSeekConfig deepSeekConfig) {
        this.deepSeekConfig = deepSeekConfig;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String chat(String message) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", deepSeekConfig.getModel());
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", message)
            ));
            requestBody.put("temperature", 0.7);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(deepSeekConfig.getApiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
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

            log.error("DeepSeek API response: {}", response.body());
            return "抱歉，我暂时无法回答你的问题。";
        } catch (Exception e) {
            log.error("调用DeepSeek API失败", e);
            return "抱歉，我暂时无法回答你的问题。";
        }
    }
}
