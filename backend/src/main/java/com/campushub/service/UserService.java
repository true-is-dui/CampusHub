package com.campushub.service;

import com.campushub.dto.user.UserMeResponse;
import com.campushub.service.dto.StoredFileContent;
import com.campushub.service.dto.UserBrief;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

/**
 * 用户服务，负责当前用户资料读取与维护，对应 {@code class_design.md} 的 {@code UserService}。
 *
 * <p>方法签名显式收 {@code userId}（由拦截器从 JWT 解析、经 {@code @CurrentUser}
 * 注入），不信任前端传入的用户标识。
 *
 * <p>用户实体的读取与认证状态变更统一从本服务出口，其他模块（如实名审核）不直接访问
 * {@code UserMapper}，以保证「用户状态变更经 owner service」的边界。实名认证的提交与
 * 审核流转由 {@code VerificationReviewService} 编排，本服务只提供用户侧的状态变更原语。
 */
public interface UserService {

    /**
     * 读取当前用户的完整资料（读库返回最新值，不依赖 JWT 中的快照）。
     *
     * @param userId 当前登录用户 ID
     * @return 契约字段齐备的 {@link UserMeResponse}，学号已脱敏
     */
    UserMeResponse getCurrentUser(Long userId);

    /**
     * 部分更新当前用户公开资料。
     *
     * <p>字段不传表示不修改；college/contact 传空字符串表示清空。
     */
    void updateProfile(Long userId, String nickname, MultipartFile avatar, String college, String contact);

    /** 公开读取用户头像；用户不存在、未设置头像或文件不存在均返回 404。 */
    StoredFileContent loadAvatar(Long userId);

    /**
     * 按 ID 批量读取用户公开摘要，供其他模块列表展示（一次 IN 查询，避免 N+1）。
     *
     * <p>只返回 {@link UserBrief}（userId/username/nickname），不暴露学号、姓名、认证材料
     * 等敏感字段（class_design.md §315 / FR-UM-03）。不存在的 ID 直接略过。
     */
    List<UserBrief> getUserBriefs(Collection<Long> userIds);

    /** 是否已有其他用户以该学号通过认证（用于提交/审核时的学号唯一性预检）。 */
    boolean isStudentIdCertified(String studentId, Long excludeUserId);

    /**
     * 实名认证提交：将用户认证状态置为审核中（REVIEWING）。
     *
     * <p>由 {@code VerificationReviewService} 在用户提交认证申请时调用；若用户已通过认证则
     * 拒绝重复提交（409）。用户状态变更归口本服务。
     */
    void markVerificationSubmitted(Long userId);

    /**
     * 实名认证审核通过：同步学号/姓名到用户并置为已认证。
     *
     * <p>由实名审核模块在管理员处理审核时调用；学号唯一性冲突在此判定（409）。
     */
    void markVerificationApproved(Long userId, String studentId, String realName);

    /** 实名认证审核驳回：将用户认证状态置为已驳回。 */
    void markVerificationRejected(Long userId);

    /**
     * 校验用户已实名认证通过、可参与代取业务（发布、接单）。
     *
     * <p>读库取权威认证状态（不信任 JWT 快照可能滞后）；未通过认证抛
     * {@code FORBIDDEN + AUTH_STATUS_NOT_ALLOWED}（HTTP 403）。供代取等业务模块在
     * 状态变更前作为前置门槛调用，状态读取归口本 owner service。
     */
    void ensureCertified(Long userId);
}


