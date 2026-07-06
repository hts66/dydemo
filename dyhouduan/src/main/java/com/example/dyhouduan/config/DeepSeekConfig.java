package com.example.dyhouduan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekConfig {
    private String apiKey = "sk-e415d25c3eb64c5e96b0e713fde7de9a";
    private String apiUrl = "https://api.deepseek.com/v1/chat/completions";
    private String model = "deepseek-chat";
}
