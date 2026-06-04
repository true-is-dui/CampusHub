package com.campushub.config;

import com.campushub.entity.User;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.UserRole;
import com.campushub.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public static final String CONTEXT_KEY = "currentUser";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // Allow unauthenticated GET on pickup hall and detail (but not /users/me/pickup-requests)
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) && uri.matches(".*/pickup-requests(/\\d+)?") && !uri.contains("/users/")) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":40101,\"message\":\"未登录或登录已过期\",\"data\":null,\"errors\":{\"reason\":\"TOKEN_MISSING_OR_EXPIRED\"}}");
            return false;
        }
        try {
            Claims claims = jwtUtil.parseToken(header.substring(7));
            Long userId = Long.parseLong(claims.getSubject());
            User user = userMapper.selectById(userId);
            if (user == null) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":40101,\"message\":\"未登录或登录已过期\",\"data\":null,\"errors\":{\"reason\":\"TOKEN_MISSING_OR_EXPIRED\"}}");
                return false;
            }
            CurrentUserContext ctx = new CurrentUserContext();
            ctx.setCurrentUserId(user.getId());
            ctx.setRole(user.getRole());
            ctx.setAuthStatus(user.getAuthStatus());
            request.setAttribute(CONTEXT_KEY, ctx);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":40101,\"message\":\"未登录或登录已过期\",\"data\":null,\"errors\":{\"reason\":\"TOKEN_MISSING_OR_EXPIRED\"}}");
            return false;
        }
    }
}
