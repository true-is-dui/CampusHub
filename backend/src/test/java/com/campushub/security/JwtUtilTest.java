package com.campushub.security;

import com.campushub.common.CurrentUserContext;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.UserRole;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link JwtUtil} 单元测试：验证签发-解析的身份还原，以及过期、篡改、缺密钥的失败行为。
 *
 * <p>纯单元测试，直接构造 {@link JwtProperties}，不加载 Spring 上下文、不依赖数据库。
 */
class JwtUtilTest {

    /** 32 字节（256 位）密钥，满足 HS256 长度要求。 */
    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private JwtUtil utilWithExpiration(Duration expiration) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpiration(expiration);
        return new JwtUtil(properties);
    }

    @Test
    void generateThenParse_restoresIdentity() {
        JwtUtil jwtUtil = utilWithExpiration(Duration.ofHours(1));

        String token = jwtUtil.generateToken(42L, UserRole.ADMIN, AuthStatus.APPROVED);
        CurrentUserContext context = jwtUtil.parse(token);

        assertThat(context.getCurrentUserId()).isEqualTo(42L);
        assertThat(context.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(context.getAuthStatus()).isEqualTo(AuthStatus.APPROVED);
        assertThat(context.isAdmin()).isTrue();
    }

    @Test
    void expiredToken_failsToParse() {
        // 负有效期使令牌签发即过期
        JwtUtil jwtUtil = utilWithExpiration(Duration.ofSeconds(-10));
        String expired = jwtUtil.generateToken(1L, UserRole.USER, AuthStatus.UNVERIFIED);

        assertThatThrownBy(() -> jwtUtil.parse(expired)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedToken_failsToParse() {
        JwtUtil jwtUtil = utilWithExpiration(Duration.ofHours(1));
        String token = jwtUtil.generateToken(1L, UserRole.USER, AuthStatus.APPROVED);

        // 改动签名段使验签失败
        String tampered = token + "tampered";

        assertThatThrownBy(() -> jwtUtil.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void blankSecret_failsFast() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("  ");

        assertThatThrownBy(() -> new JwtUtil(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret");
    }
}
