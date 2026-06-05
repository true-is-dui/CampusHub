package com.campushub.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 相关配置，绑定 {@code application.yaml} 中 {@code jwt.*} 前缀的配置项。
 *
 * <p>{@code @ConfigurationProperties} 让 Spring Boot 把外部配置按字段名自动注入到本对象，
 * 比到处写 {@code @Value("${jwt.secret}")} 更集中、更易类型校验。本类由
 * {@code SecurityConfig} 上的 {@code @EnableConfigurationProperties} 注册为 Bean。
 *
 * <p>密钥不在仓库内硬编码：{@code application.yaml} 用占位符 {@code ${JWT_SECRET:}}，
 * 真实值放本机 {@code application-local.yaml}（已 gitignore）或部署环境变量。
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * HMAC 签名密钥。HS256 要求密钥长度不少于 256 位（32 字节），否则 jjwt 会拒绝。
     */
    private String secret;

    /**
     * Token 有效期。Spring Boot 支持 {@code 24h}、{@code 30m} 这类写法自动转 Duration。
     */
    private Duration expiration = Duration.ofHours(24);
}
