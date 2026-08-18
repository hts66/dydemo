package com.example.dyhouduan.service.impl;

import com.example.dyhouduan.exception.VerificationCodeRateLimitException;
import com.example.dyhouduan.service.EmailService;
import com.example.dyhouduan.service.VerificationCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final int CODE_LENGTH = 6;
    private static final long CODE_TTL_MINUTES = 5;
    private static final long SEND_COOLDOWN_SECONDS = 60;
    private static final String CODE_KEY_PREFIX = "email_code:";
    private static final String COOLDOWN_KEY_PREFIX = "email_code:cooldown:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private EmailService emailService;

    @Override
    public String generateCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        String cooldownKey = COOLDOWN_KEY_PREFIX + normalizedEmail;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                cooldownKey,
                "1",
                Duration.ofSeconds(SEND_COOLDOWN_SECONDS)
        );

        if (!Boolean.TRUE.equals(acquired)) {
            Long remainingSeconds = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
            long retryAfter = remainingSeconds != null && remainingSeconds > 0
                    ? remainingSeconds
                    : SEND_COOLDOWN_SECONDS;
            throw new VerificationCodeRateLimitException(retryAfter);
        }

        String code = generateRandomCode();
        String codeKey = CODE_KEY_PREFIX + normalizedEmail;

        try {
            redisTemplate.opsForValue().set(
                    codeKey,
                    code,
                    Duration.ofMinutes(CODE_TTL_MINUTES)
            );
            emailService.sendVerificationCode(normalizedEmail, code);
            return code;
        } catch (RuntimeException e) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(cooldownKey);
            throw e;
        }
    }

    @Override
    public boolean verifyCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        String codeKey = CODE_KEY_PREFIX + normalizedEmail;
        Object storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            return false;
        }
        
        String storedValue = storedCode.toString();
        if (storedValue.isEmpty()) {
            return false;
        }
        
        // Keep codes created before this deployment valid during the rollout.
        if (storedValue.contains(":")) {
            String[] parts = storedValue.split(":");
            if (parts.length < 2) {
                return false;
            }

            long timestamp = Long.parseLong(parts[1]);
            if (System.currentTimeMillis() - timestamp > CODE_TTL_MINUTES * 60 * 1000) {
                redisTemplate.delete(codeKey);
                return false;
            }

            storedValue = parts[0];
        }

        return storedValue.equals(code);
    }

    @Override
    public void clearCode(String email) {
        redisTemplate.delete(CODE_KEY_PREFIX + normalizeEmail(email));
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
