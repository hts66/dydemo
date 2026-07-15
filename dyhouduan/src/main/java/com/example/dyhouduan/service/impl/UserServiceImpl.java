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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private FileStorageUtil fileStorageUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

        if (!passwordEncoder.matches(password, user.getPassword())) {
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
                user.getAvatar(),
                user.getBackground()
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
        user.setPassword(passwordEncoder.encode(request.getPassword()));
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

    @Override
    public User resetPassword(String email, String newPassword) {
        User user = findByEmail(email);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
        return user;
    }

    @Override
    public User updateBackground(Long userId, String background) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 删除旧的背景文件
        String oldBg = user.getBackground();
        if (oldBg != null && !oldBg.isEmpty()) {
            if (useOss()) {
                ossUtil.deleteFile(oldBg);
            } else {
                fileStorageUtil.deleteFile(oldBg);
            }
        }
        user.setBackground(background);
        this.updateById(user);
        return user;
    }

    @Override
    public User updateAvatar(Long userId, MultipartFile file) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 上传新头像到 OSS（或本地）
        String newAvatarUrl;
        if (useOss()) {
            newAvatarUrl = ossUtil.uploadFile(file, "avatars");
        } else {
            newAvatarUrl = fileStorageUtil.uploadFile(file, "avatars");
        }

        // 删除旧头像
        String oldAvatar = user.getAvatar();
        if (oldAvatar != null && !oldAvatar.isEmpty()) {
            if (useOss()) {
                ossUtil.deleteFile(oldAvatar);
            } else {
                fileStorageUtil.deleteFile(oldAvatar);
            }
        }

        // 更新用户头像
        user.setAvatar(newAvatarUrl);
        this.updateById(user);
        return user;
    }

    @Override
    public User updateBackgroundFile(Long userId, MultipartFile file) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 上传新背景图到 OSS（或本地）
        String newBgUrl;
        if (useOss()) {
            newBgUrl = ossUtil.uploadFile(file, "backgrounds");
        } else {
            newBgUrl = fileStorageUtil.uploadFile(file, "backgrounds");
        }

        // 删除旧背景图
        String oldBg = user.getBackground();
        if (oldBg != null && !oldBg.isEmpty()) {
            if (useOss()) {
                ossUtil.deleteFile(oldBg);
            } else {
                fileStorageUtil.deleteFile(oldBg);
            }
        }

        // 更新用户背景图
        user.setBackground(newBgUrl);
        this.updateById(user);
        return user;
    }
}
