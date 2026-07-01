package com.example.dyhouduan.service;

public interface EmailService {
    void sendVerificationCode(String email, String code);
}