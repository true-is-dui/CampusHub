package com.campushub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全相关 Bean 与配置装配。
 *
 * <p>本项目不引入 Spring Security 过滤器链，仅复用其 {@link BCryptPasswordEncoder}
 * 做密码哈希，鉴权由轻量拦截器方案完成（见 {@code security} 包）。
 *
 * <p>{@code JwtProperties} 等配置类的注册集中在 {@link PropertiesConfig}。
 */
@Configuration
public class SecurityConfig {

    /**
     * 密码编码器：BCrypt 自带随机盐，{@code encode} 每次输出不同，
     * 校验用 {@code matches(raw, encoded)}。注册保存密码时哈希，登录时比对，杜绝明文存储。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
