package com.example.dyhouduan.exception;

public class VerificationCodeRateLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    public VerificationCodeRateLimitException(long retryAfterSeconds) {
        super("验证码发送过于频繁");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
