package com.campushub.dto.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 登录成功响应数据，对应 {@code api_design.yaml} 的 {@code LoginSession}。
 *
 * <p>认证接口只返回会话令牌，完整用户资料由前端携带 token 调用 {@code GET /users/me}
 * 获取。设计为不可变对象（{@code final} 字段 + Lombok 生成的全参构造），令牌签发后不再变更。
 */
@Getter
@RequiredArgsConstructor
public class LoginSession {

    /** 会话令牌（JWT）。 */
    private final String token;
}
