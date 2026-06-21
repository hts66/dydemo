package com.example.dyhouduan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dyhouduan.dto.LoginResponse;
import com.example.dyhouduan.dto.RegisterRequest;
import com.example.dyhouduan.entity.User;
import com.example.dyhouduan.mapper.UserMapper;
import com.example.dyhouduan.service.UserService;
import com.example.dyhouduan.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public LoginResponse login(String email, String password) {
        User user = findByEmail(email);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        LoginResponse.UserVO userVO = new LoginResponse.UserVO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getGender(),
                user.getBio(),
                user.getAvatar()
        );

        return new LoginResponse(token, userVO);
    }

    @Override
    public User register(RegisterRequest request) {
        User existUser = findByEmail(request.getEmail());
        if (existUser != null) {
            throw new RuntimeException("邮箱已被注册");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setUsername(request.getUsername());
        user.setGender(0);
        user.setBio("");
        user.setAvatar("");

        this.save(user);
        return user;
    }

    @Override
    public User findByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return this.getOne(wrapper);
    }

    @Override
    public User updateProfile(Long userId, User profile) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (profile.getUsername() != null) {
            user.setUsername(profile.getUsername());
        }
        if (profile.getGender() != null) {
            user.setGender(profile.getGender());
        }
        if (profile.getBio() != null) {
            user.setBio(profile.getBio());
        }
        if (profile.getAvatar() != null) {
            user.setAvatar(profile.getAvatar());
        }
        this.updateById(user);
        return user;
    }
}
