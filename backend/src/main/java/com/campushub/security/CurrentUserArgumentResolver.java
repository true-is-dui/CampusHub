package com.campushub.security;

import com.campushub.common.BusinessException;
import com.campushub.common.CurrentUserContext;
import com.campushub.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 解析 {@code @CurrentUser CurrentUserContext} 参数：从请求属性取出
 * {@link AuthInterceptor} 解析 Token 后放入的当前用户上下文，注入到 Controller 方法参数。
 *
 * <p>这是 Spring MVC 的扩展点 {@link HandlerMethodArgumentResolver}：框架在调用
 * Controller 方法前，对每个被 {@link #supportsParameter} 认领的参数调用
 * {@link #resolveArgument} 取值。由此把"取当前用户"的逻辑收敛到一处。
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    /** 仅认领同时满足"带 @CurrentUser 注解"且"类型为 CurrentUserContext"的参数。 */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && CurrentUserContext.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        Object context = request == null ? null
                : request.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        if (context == null) {
            // 受保护接口必经拦截器，正常不会为空；为空说明该路径漏配鉴权或被错误放行
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return context;
    }
}
