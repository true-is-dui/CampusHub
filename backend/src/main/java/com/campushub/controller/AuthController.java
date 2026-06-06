package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.dto.user.LoginRequest;
import com.campushub.dto.user.LoginSession;
import com.campushub.dto.user.RegisterRequest;
import com.campushub.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册与登录。无需登录即可访问（白名单 {@code /auth/**}）。
 *
 * <p>Controller 只负责：触发 {@code @Valid} 入参校验、调用服务、用 {@link ApiResponse}
 * 包装成功响应。参数校验失败与业务异常由 {@code GlobalExceptionHandler} 统一翻译，
 * 此处不写任何错误处理分支。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 注册成功不签发 token，返回空 data，前端跳转登录页。 */
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.getUsername(), request.getPassword());
        return ApiResponse.ok();
    }

    /** 登录成功返回会话令牌；完整资料由前端携 token 调 GET /users/me 获取。 */
    @PostMapping("/login")
    public ApiResponse<LoginSession> login(@Valid @RequestBody LoginRequest request) {
        LoginSession session = authService.login(request.getUsername(), request.getPassword());
        return ApiResponse.ok(session);
    }
}
