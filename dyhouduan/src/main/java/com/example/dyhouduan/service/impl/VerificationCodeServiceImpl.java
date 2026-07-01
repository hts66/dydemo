package com.example.dyhouduan.service.impl;

import com.example.dyhouduan.service.EmailService;
import com.example.dyhouduan.service.VerificationCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 5;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private EmailService emailService;

    @Override
    public String generateCode(String email) {
        String code = generateRandomCode();
        redisTemplate.opsForValue().set("email_code:" + email, code, EXPIRATION_MINUTES, TimeUnit.MINUTES);
        emailService.sendVerificationCode(email, code);
        return code;
    }

    @Override
    public boolean verifyCode(String email, String code) {
        Object storedCode = redisTemplate.opsForValue().get("email_code:" + email);
        if (storedCode == null) {
            return false;
        }
        boolean valid = storedCode.equals(code);
        if (valid) {
            redisTemplate.delete("email_code:" + email);
        }
        return valid;
    }

    @Override
    public void clearCode(String email) {
        redisTemplate.delete("email_code:" + email);
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