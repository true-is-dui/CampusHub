package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.dto.LoginRequest;
import com.campushub.dto.LoginSession;
import com.campushub.dto.RegisterRequest;
import com.campushub.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok();
    }

    @PostMapping("/login")
    public ApiResponse<LoginSession> login(@RequestBody LoginRequest request) {
        LoginSession session = authService.login(request);
        return ApiResponse.ok(session);
    }
}
