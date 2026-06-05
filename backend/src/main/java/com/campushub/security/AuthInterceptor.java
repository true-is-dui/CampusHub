package com.campushub.security;

import com.campushub.common.BusinessException;
import com.campushub.common.CurrentUserContext;
import com.campushub.common.ErrorCode;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器：在受保护接口的 Controller 执行前校验 JWT。
 *
 * <p>职责单一——只做"验签 + 还原身份"，不查库、不拼接 JSON、不写路径正则：
 * <ol>
 *   <li>从 {@code Authorization: Bearer <token>} 取令牌；缺失或格式不符 → 认证失败；</li>
 *   <li>用 {@link JwtUtil#parse} 验签解析；失败 → 认证失败；</li>
 *   <li>成功则把 {@link CurrentUserContext} 放入请求属性，供
 *       {@link CurrentUserArgumentResolver} 注入到 {@code @CurrentUser} 参数。</li>
 * </ol>
 *
 * <p>抛出的 {@link BusinessException} 由全局异常处理器统一翻译为 40101 响应，
 * 因此这里不直接写响应体。放行白名单（如 {@code /auth/**}）在 WebMvcConfig 中配置。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    /** 请求属性 key：拦截器写入、参数解析器读取的当前用户上下文。 */
    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS 预检请求不携带令牌，直接放行交给 CORS 处理
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            CurrentUserContext context = jwtUtil.parse(token);
            request.setAttribute(CURRENT_USER_ATTRIBUTE, context);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // 令牌无效、过期、被篡改或载荷格式异常
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
    }
}
