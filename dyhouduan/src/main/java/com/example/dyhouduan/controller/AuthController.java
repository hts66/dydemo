package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.LoginRequest;
import com.example.dyhouduan.dto.LoginResponse;
import com.example.dyhouduan.dto.RegisterRequest;
import com.example.dyhouduan.dto.RefreshTokenRequest;
import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.entity.User;
import com.example.dyhouduan.service.UserService;
import com.example.dyhouduan.service.VerificationCodeService;
import com.example.dyhouduan.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/login")
    public Response<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            if (!validateCaptcha(request.getCaptchaKey(), request.getCaptcha())) {
                return Response.error(400, "图形验证码错误");
            }

            if (!verificationCodeService.verifyCode(request.getEmail(), request.getCode())) {
                return Response.error(400, "邮箱验证码错误或已过期");
            }

            LoginResponse loginResponse = userService.login(request.getEmail(), request.getPassword());
            return Response.success(loginResponse);
        } catch (Exception e) {
            return Response.error(401, e.getMessage());
        }
    }

    @PostMapping("/login/code")
    public Response<LoginResponse> loginWithCode(@RequestBody LoginRequest request) {
        try {
            if (!validateCaptcha(request.getCaptchaKey(), request.getCaptcha())) {
                return Response.error(400, "图形验证码错误");
            }

            if (!verificationCodeService.verifyCode(request.getEmail(), request.getCode())) {
                return Response.error(400, "验证码错误或已过期");
            }

            User user = userService.findByEmail(request.getEmail());
            if (user == null) {
                return Response.error(401, "用户不存在");
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

            return Response.success(new LoginResponse(token, refreshToken, userVO));
        } catch (Exception e) {
            return Response.error(401, e.getMessage());
        }
    }

    @PostMapping("/register")
    public Response<User> register(@Valid @RequestBody RegisterRequest request) {
        try {
            if (!validateCaptcha(request.getCaptchaKey(), request.getCaptcha())) {
                return Response.error(400, "图形验证码错误");
            }

            if (!verificationCodeService.verifyCode(request.getEmail(), request.getCode())) {
                return Response.error(400, "验证码错误或已过期");
            }

            User user = userService.register(request);
            return Response.success("注册成功", user);
        } catch (Exception e) {
            return Response.error(400, e.getMessage());
        }
    }

    @PostMapping("/register/code")
    public Response<User> registerWithCode(@RequestBody RegisterRequest request) {
        try {
            if (!validateCaptcha(request.getCaptchaKey(), request.getCaptcha())) {
                return Response.error(400, "图形验证码错误");
            }

            if (!verificationCodeService.verifyCode(request.getEmail(), request.getCode())) {
                return Response.error(400, "验证码错误或已过期");
            }

            User user = userService.register(request);
            return Response.success("注册成功", user);
        } catch (Exception e) {
            return Response.error(400, e.getMessage());
        }
    }

    @PostMapping("/send-code")
    public Response<String> sendCode(@RequestBody SendCodeRequest request) {
        try {
            verificationCodeService.generateCode(request.getEmail());
            return Response.success("验证码已发送");
        } catch (Exception e) {
            return Response.error(500, "发送验证码失败: " + e.getMessage());
        }
    }

    @PostMapping("/verify-code")
    public Response<String> verifyCode(@RequestBody VerifyCodeRequest request) {
        try {
            boolean valid = verificationCodeService.verifyCode(request.getEmail(), request.getCode());
            if (valid) {
                return Response.success("验证码验证成功");
            } else {
                return Response.error(400, "验证码错误或已过期");
            }
        } catch (Exception e) {
            return Response.error(400, e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public Response<String> forgotPassword(@RequestBody SendCodeRequest request) {
        try {
            User user = userService.findByEmail(request.getEmail());
            if (user == null) {
                return Response.error(400, "该邮箱未注册");
            }

            verificationCodeService.generateCode(request.getEmail());
            return Response.success("验证码已发送");
        } catch (Exception e) {
            return Response.error(500, "发送验证码失败: " + e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public Response<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            if (!validateCaptcha(request.getCaptchaKey(), request.getCaptcha())) {
                return Response.error(400, "图形验证码错误");
            }

            if (!verificationCodeService.verifyCode(request.getEmail(), request.getCode())) {
                return Response.error(400, "验证码错误或已过期");
            }

            userService.resetPassword(request.getEmail(), request.getNewPassword());
            return Response.success("密码重置成功");
        } catch (Exception e) {
            return Response.error(400, e.getMessage());
        }
    }

    @PutMapping("/user/profile")
    public Response<User> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody User profile) {
        try {
            String token = authorization != null && authorization.startsWith("Bearer ")
                    ? authorization.substring(7) : authorization;
            Long userId = jwtUtil.getUserId(token);
            User updated = userService.updateProfile(userId, profile);
            return Response.success("更新成功", updated);
        } catch (Exception e) {
            return Response.error(400, e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public Response<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                return Response.error(400, "刷新令牌不能为空");
            }

            if (jwtUtil.isTokenExpired(refreshToken)) {
                return Response.error(401, "刷新令牌已过期，请重新登录");
            }

            Claims claims = jwtUtil.parseToken(refreshToken);
            String type = claims.get("type", String.class);
            if (!"refresh".equals(type)) {
                return Response.error(400, "无效的刷新令牌");
            }

            Long userId = jwtUtil.getUserId(refreshToken);
            String email = jwtUtil.getEmail(refreshToken);

            String newAccessToken = jwtUtil.generateAccessToken(userId, email);
            String newRefreshToken = jwtUtil.generateRefreshToken(userId, email);

            User user = userService.getById(userId);
            LoginResponse.UserVO userVO = new LoginResponse.UserVO(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getGender(),
                    user.getBio(),
                    user.getAvatar()
            );

            return Response.success(new LoginResponse(newAccessToken, newRefreshToken, userVO));
        } catch (Exception e) {
            return Response.error(401, "刷新令牌无效，请重新登录");
        }
    }

    private boolean validateCaptcha(String captchaKey, String captcha) {
        if (captchaKey == null || captcha == null || captcha.isEmpty()) {
            return false;
        }
        Object storedCaptcha = redisTemplate.opsForValue().get("captcha:" + captchaKey);
        if (storedCaptcha == null) {
            return false;
        }
        boolean valid = storedCaptcha.toString().equalsIgnoreCase(captcha.trim());
        if (valid) {
            redisTemplate.delete("captcha:" + captchaKey);
        }
        return valid;
    }

    public static class SendCodeRequest {
        private String email;
        private String captcha;
        private String captchaKey;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCaptcha() {
            return captcha;
        }

        public void setCaptcha(String captcha) {
            this.captcha = captcha;
        }

        public String getCaptchaKey() {
            return captchaKey;
        }

        public void setCaptchaKey(String captchaKey) {
            this.captchaKey = captchaKey;
        }
    }

    public static class VerifyCodeRequest {
        private String email;
        private String code;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    public static class ResetPasswordRequest {
        private String email;
        private String code;
        private String newPassword;
        private String captcha;
        private String captchaKey;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        public String getCaptcha() {
            return captcha;
        }

        public void setCaptcha(String captcha) {
            this.captcha = captcha;
        }

        public String getCaptchaKey() {
            return captchaKey;
        }

        public void setCaptchaKey(String captchaKey) {
            this.captchaKey = captchaKey;
        }
    }
}