package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.LoginRequest;
import com.example.dyhouduan.dto.LoginResponse;
import com.example.dyhouduan.dto.RegisterRequest;
import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.entity.User;
import com.example.dyhouduan.service.UserService;
import com.example.dyhouduan.utils.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Response<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse loginResponse = userService.login(request.getEmail(), request.getPassword());
            return Response.success(loginResponse);
        } catch (Exception e) {
            return Response.error(401, e.getMessage());
        }
    }

    @PostMapping("/register")
    public Response<User> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request);
            return Response.success("注册成功", user);
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
}
