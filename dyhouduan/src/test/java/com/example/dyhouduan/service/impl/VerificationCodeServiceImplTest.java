package com.example.dyhouduan.service.impl;

import com.example.dyhouduan.exception.VerificationCodeRateLimitException;
import com.example.dyhouduan.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private VerificationCodeServiceImpl verificationCodeService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void sendsOnlyOneEmailDuringCooldown() {
        String cooldownKey = "email_code:cooldown:test@example.com";
        when(valueOperations.setIfAbsent(
                cooldownKey,
                "1",
                Duration.ofSeconds(60)
        )).thenReturn(true, false);
        when(redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS)).thenReturn(47L);

        verificationCodeService.generateCode(" Test@Example.com ");
        VerificationCodeRateLimitException exception = assertThrows(
                VerificationCodeRateLimitException.class,
                () -> verificationCodeService.generateCode("test@example.com")
        );

        assertEquals(47L, exception.getRetryAfterSeconds());
        verify(emailService, times(1))
                .sendVerificationCode(eq("test@example.com"), anyString());
        verify(valueOperations, times(1)).set(
                eq("email_code:test@example.com"),
                anyString(),
                eq(Duration.ofMinutes(5))
        );
    }
}
