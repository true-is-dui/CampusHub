package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.CurrentUserContext;
import com.campushub.dto.user.UserMeResponse;
import com.campushub.security.CurrentUser;
import com.campushub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户接口。需登录访问，身份由 {@code @CurrentUser} 从 JWT 解析注入，
 * 不接受前端传入的用户标识。
 *
 * <p>本批仅实现 {@code GET /users/me}；资料编辑与实名认证依赖文件模块，留待后续。
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 获取当前登录用户的完整资料（读库返回最新值）。 */
    @GetMapping("/me")
    public ApiResponse<UserMeResponse> getCurrentUser(@CurrentUser CurrentUserContext me) {
        return ApiResponse.ok(userService.getCurrentUser(me.getCurrentUserId()));
    }
}
