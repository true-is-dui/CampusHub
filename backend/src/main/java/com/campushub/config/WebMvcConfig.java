package com.campushub.config;

import com.campushub.security.AuthInterceptor;
import com.campushub.security.CurrentUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC 装配：注册鉴权拦截器、{@code @CurrentUser} 参数解析器与开发期 CORS。
 *
 * <p>路径均相对于 {@code context-path}（即 {@code /api} 之后的部分），
 * 因此白名单写 {@code /auth/**} 即可匹配 {@code /api/auth/login} 等登录注册接口。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    /** 无需登录即可访问的路径：注册、登录，以及容器内部错误转发。 */
    private static final String[] AUTH_WHITELIST = {
            "/auth/**",
            "/users/*/avatar",
            "/error"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(AUTH_WHITELIST);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    /**
     * 开发期 CORS：允许前端开发服务器跨域调用。鉴权走 Authorization 头（非 Cookie），
     * 故无需 {@code allowCredentials}。生产部署应改为按实际前端域名收紧来源。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*");
    }
}
