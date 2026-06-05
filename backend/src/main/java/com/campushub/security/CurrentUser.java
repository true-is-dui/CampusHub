package com.campushub.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Controller 方法参数上，表示该参数应注入当前登录用户上下文
 * （{@link com.campushub.common.CurrentUserContext}）。
 *
 * <p>由 {@link CurrentUserArgumentResolver} 在方法调用前完成注入，
 * 免去到处手写 {@code request.getAttribute(...)} 的样板代码。
 *
 * <p>用法：{@code public ApiResponse<...> publish(@CurrentUser CurrentUserContext me, ...)}
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
