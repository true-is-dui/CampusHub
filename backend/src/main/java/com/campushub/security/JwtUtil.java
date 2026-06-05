package com.campushub.security;

import com.campushub.common.CurrentUserContext;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌工具：签发与解析。基于 jjwt 0.12.x。
 *
 * <p>令牌载荷携带身份快照——{@code sub}=用户 ID、{@code role}=角色、
 * {@code authStatus}=认证状态。这样拦截器只需验签即可还原 {@link CurrentUserContext}，
 * 无需每次请求查库；需要权威认证状态的业务门槛由服务层另行读库校验。
 *
 * <p>签名密钥从 {@link JwtProperties} 注入，仓库内不出现明文密钥。
 */
@Component
public class JwtUtil {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_AUTH_STATUS = "authStatus";

    /** HMAC 密钥，构造时一次性生成，复用以避免每次签发/解析重复构建。 */
    private final SecretKey key;

    /** 有效期（毫秒），由 {@link JwtProperties#getExpiration()} 换算。 */
    private final long expirationMillis;

    public JwtUtil(JwtProperties properties) {
        if (!StringUtils.hasText(properties.getSecret())) {
            throw new IllegalStateException(
                    "jwt.secret 未配置：请在环境变量 JWT_SECRET 或 application-local.yaml 中提供（HS256 需至少 32 字节）");
        }
        // hmacShaKeyFor 会校验密钥长度，过短（<256 位）将抛 WeakKeyException
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = properties.getExpiration().toMillis();
    }

    /**
     * 签发令牌，把用户身份快照写入载荷。
     *
     * @param userId     用户 ID，作为标准 {@code sub} 声明
     * @param role       用户角色
     * @param authStatus 认证状态快照
     */
    public String generateToken(Long userId, UserRole role, AuthStatus authStatus) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_AUTH_STATUS, authStatus.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 验签并解析令牌为当前用户上下文。
     *
     * <p>令牌无效（签名不符、已过期、格式错误）时，jjwt 会抛
     * {@link io.jsonwebtoken.JwtException}，由调用方（拦截器）翻译成认证失败响应。
     */
    public CurrentUserContext parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId = Long.valueOf(claims.getSubject());
        UserRole role = UserRole.valueOf(claims.get(CLAIM_ROLE, String.class));
        AuthStatus authStatus = AuthStatus.valueOf(claims.get(CLAIM_AUTH_STATUS, String.class));
        return new CurrentUserContext(userId, role, authStatus);
    }
}
