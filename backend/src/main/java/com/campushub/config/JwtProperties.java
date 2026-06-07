package com.campushub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 相关配置，绑定 {@code application.yaml} 中 {@code jwt.*} 前缀的配置项。
 *
 * <p>{@code @ConfigurationProperties} 让 Spring Boot 把外部配置按字段名自动注入到本对象，
 * 比到处写 {@code @Value("${jwt.secret}")} 更集中、更易类型校验。注册见 {@link PropertiesConfig}。
 *
 * <p>用 {@code @Getter @Setter} 而非 {@code @Data}：避免 {@code @ToString} 把 {@link #secret}
 * 打进日志造成密钥泄露。密钥不在仓库内硬编码：{@code application.yaml} 用占位符
 * {@code ${JWT_SECRET:}}，真实值放本机 {@code application-local.yaml}（已 gitignore）或部署环境变量。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * HMAC 签名密钥。HS256 要求密钥长度不少于 256 位（32 字节），否则 jjwt 会拒绝。
     * 不设缺省值：必须由外部强制提供，杜绝弱默认密钥。
     */
    private String secret;

    /**
     * Token 有效期。Spring Boot 支持 {@code 24h}、{@code 30m} 这类写法自动转 Duration。
     * 配置缺省值为 24 小时。
     */
    private Duration expiration = Duration.ofHours(24);
}
