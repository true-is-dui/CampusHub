package com.campushub.common;

import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.UserRole;
import lombok.Getter;

/**
 * 当前登录用户上下文，对应 {@code class_design.md} 中的 CurrentUserContext。
 *
 * <p>由 JWT 拦截器解析 Token 后生成并放入请求，再经 {@code @CurrentUser} 注入到
 * Controller 方法参数。业务服务只从这里读取身份，<b>不信任前端传入的用户 ID</b>，
 * 以此杜绝越权伪造。
 *
 * <p>不可变值对象：三个字段均为 {@code final}，构造后不可改，天然线程安全。
 */
@Getter
public class CurrentUserContext {

    /** 当前登录用户 ID。 */
    private final Long currentUserId;

    /** 当前用户角色，用于区分普通用户与管理员（实名审核入口）。 */
    private final UserRole role;

    /** 当前用户认证状态的 Token 快照；权威校验仍以服务层读库为准。 */
    private final AuthStatus authStatus;

    public CurrentUserContext(Long currentUserId, UserRole role, AuthStatus authStatus) {
        this.currentUserId = currentUserId;
        this.role = role;
        this.authStatus = authStatus;
    }

    /** 是否为管理员，便于 Service 做角色判断而不散落字符串比较。 */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
