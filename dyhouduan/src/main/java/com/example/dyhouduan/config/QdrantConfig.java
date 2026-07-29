package com.example.dyhouduan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "qdrant")
public class QdrantConfig {
    private String host = "localhost";
    private int port = 6333;
    private String apiKey = "";
    private boolean useTls = false;

    public String getBaseUrl() {
        return (useTls ? "https" : "http") + "://" + host + ":" + port;
    }
}
