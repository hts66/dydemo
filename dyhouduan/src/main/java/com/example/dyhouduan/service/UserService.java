package com.example.dyhouduan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dyhouduan.dto.LoginResponse;
import com.example.dyhouduan.dto.RegisterRequest;
import com.example.dyhouduan.entity.User;

public interface UserService extends IService<User> {
    LoginResponse login(String email, String password);

    User register(RegisterRequest request);

    User findByEmail(String email);

    User updateProfile(Long userId, User profile);

    User resetPassword(String email, String newPassword);
}
