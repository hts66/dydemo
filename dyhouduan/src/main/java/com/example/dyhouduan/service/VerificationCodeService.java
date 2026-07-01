package com.example.dyhouduan.service;

public interface VerificationCodeService {
    String generateCode(String email);
    boolean verifyCode(String email, String code);
    void clearCode(String email);
}