package com.campushub.service;

import com.campushub.dto.user.UserMeResponse;

/**
 * 用户服务，负责当前用户资料读取与维护，对应 {@code class_design.md} 的 {@code UserService}。
 *
 * <p>本批先实现 {@code getCurrentUser}；资料编辑、实名认证提交（含文件上传）
 * 依赖文件模块，留待文件模块就绪后补充。
 *
 * <p>方法签名显式收 {@code userId}（由拦截器从 JWT 解析、经 {@code @CurrentUser}
 * 注入），不信任前端传入的用户标识。
 */
public interface UserService {

    /**
     * 读取当前用户的完整资料（读库返回最新值，不依赖 JWT 中的快照）。
     *
     * @param userId 当前登录用户 ID
     * @return 契约字段齐备的 {@link UserMeResponse}，学号已脱敏
     */
    UserMeResponse getCurrentUser(Long userId);
}
