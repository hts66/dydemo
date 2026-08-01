package com.example.dyhouduan.service;

import java.util.List;
import java.util.Map;

public interface DeepSeekService {
    String chat(String message);
    String chatWithHistory(List<Map<String, String>> messages);
    /** 视觉分析：发送图片(base64) + 文本提示词，返回分析结果 */
    String chatVision(String prompt, List<String> base64Images);
}
