package com.campushub.service;

import com.campushub.dto.user.LoginSession;

/**
 * 认证服务，负责注册与登录，对应 {@code class_design.md} 的 {@code AuthService}。
 *
 * <p>签名以原始字段入参（而非 Web 请求 DTO），使服务层与具体接口请求对象解耦：
 * Controller 完成 {@code @Valid} 参数校验后，取出用户名密码调用本服务。
 * 密码哈希、用户名唯一性校验、JWT 签发等业务规则集中在实现类。
 */
public interface AuthService {

    /**
     * 用户名密码注册：校验用户名唯一，保存加盐密码哈希。
     *
     * @param username    登录账号（已通过格式校验）
     * @param rawPassword 原始密码（已通过格式校验），实现内加盐哈希后存储
     */
    void register(String username, String rawPassword);

    /**
     * 用户名密码登录：用户存在且密码正确时签发 JWT。
     *
     * @param username    登录账号
     * @param rawPassword 原始密码，与存储的哈希比对
     * @return 含会话令牌的 {@link LoginSession}
     */
    LoginSession login(String username, String rawPassword);
}
