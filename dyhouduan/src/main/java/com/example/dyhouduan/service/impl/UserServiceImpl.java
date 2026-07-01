package com.example.dyhouduan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dyhouduan.dto.LoginResponse;
import com.example.dyhouduan.dto.RegisterRequest;
import com.example.dyhouduan.entity.User;
import com.example.dyhouduan.mapper.UserMapper;
import com.example.dyhouduan.service.UserService;
import com.example.dyhouduan.utils.FileStorageUtil;
import com.example.dyhouduan.utils.JwtUtil;
import com.example.dyhouduan.utils.OssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private FileStorageUtil fileStorageUtil;

    @Value("${aliyun.oss.endpoint:}")
    private String ossEndpoint;

    private boolean useOss() {
        return ossEndpoint != null && !ossEndpoint.isEmpty();
    }

    @Override
    public LoginResponse login(String email, String password) {
        User user = findByEmail(email);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("密码错误");
        }

        String token = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        LoginResponse.UserVO userVO = new LoginResponse.UserVO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getGender(),
                user.getBio(),
                user.getAvatar()
        );

        return new LoginResponse(token, refreshToken, userVO);
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
            String oldAvatar = user.getAvatar();
            user.setAvatar(profile.getAvatar());
            if (oldAvatar != null && !oldAvatar.isEmpty()) {
                if (useOss()) {
                    ossUtil.deleteFile(oldAvatar);
                } else {
                    fileStorageUtil.deleteFile(oldAvatar);
                }
            }
        }
        this.updateById(user);
        return user;
    }
}
