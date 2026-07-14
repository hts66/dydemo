package com.example.dyhouduan.service.impl;

import com.example.dyhouduan.service.EmailService;
import com.example.dyhouduan.service.VerificationCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final int CODE_LENGTH = 6;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private EmailService emailService;

    @Override
    public String generateCode(String email) {
        String code = generateRandomCode();
        long timestamp = System.currentTimeMillis();
        String value = code + ":" + timestamp;
        redisTemplate.opsForValue().set("email_code:" + email, value);
        emailService.sendVerificationCode(email, code);
        return code;
    }

    @Override
    public boolean verifyCode(String email, String code) {
        Object storedCode = redisTemplate.opsForValue().get("email_code:" + email);
        if (storedCode == null) {
            return false;
        }
        
        String storedValue = storedCode.toString();
        if (storedValue.isEmpty()) {
            return false;
        }
        
        String[] parts = storedValue.split(":");
        if (parts.length < 2) {
            return false;
        }
        
        String storedCodeValue = parts[0];
        long timestamp = Long.parseLong(parts[1]);
        long now = System.currentTimeMillis();
        
        if (now - timestamp > 5 * 60 * 1000) {
            redisTemplate.opsForValue().set("email_code:" + email, "");
            return false;
        }
        
        return storedCodeValue.equals(code);
    }

    @Override
    public void clearCode(String email) {
        redisTemplate.opsForValue().set("email_code:" + email, "");
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}