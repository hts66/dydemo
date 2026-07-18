package com.example.dyhouduan.service;

import java.util.List;
import java.util.Map;

public interface DeepSeekService {
    String chat(String message);
    String chatWithHistory(List<Map<String, String>> messages);
}
