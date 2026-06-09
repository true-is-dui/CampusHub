package com.campushub.service;

import com.campushub.dto.user.UserMeResponse;
import com.campushub.dto.user.UserPublicProfile;
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
     * 读取用户公开主页（昵称/学院/联系方式），公开访问。
     *
     * <p>只暴露公开资料，绝不含学号/真实姓名/认证材料（FR-UM-03）。评价摘要不在本方法内组装：
     * {@code ratingSummary} 由 Controller 按 {@code includeRating} 决定是否经评价服务填充，
     * 保持用户模块不反向依赖评价模块。用户不存在抛 404。
     */
    UserPublicProfile getPublicProfile(Long userId);

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

    /**
     * 读取用户当前积分余额（读库，不信任 JWT 快照）；用户不存在抛 404。
     *
     * <p>积分余额字段（{@code point_balance}）物理寄存在 users 表，故其读取归口本 owner service；
     * 其他模块（如 {@code PointService}）不直接访问 {@code UserMapper}。
     */
    long getPointBalance(Long userId);

    /**
     * 应用一次积分变动并落库，返回变动后的余额（balanceAfter，供调用方写流水）。
     *
     * <p>用户表写入收口本 owner service：读用户 → {@code User.addPoints/deductPoints}（领域方法）
     * → <b>条件更新</b>（{@code WHERE point_balance=旧值}，防并发超扣，与代取状态条件更新同手法）。
     *
     * <p>{@code checkInDate != null} 表示这是一次签到变动：同笔写入 {@code last_check_in_date}，
     * 并带「旧签到日期」条件（{@code WHERE last_check_in_date IS NULL OR <> checkInDate}），
     * 影响行数 0 视为当日已签到 → 抛 409 {@code ALREADY_CHECKED_IN_TODAY}。
     * {@code checkInDate == null} 表示普通积分变动：仅带余额条件，影响行数 0 视为并发冲突。
     *
     * @param delta       变动量（正=入账，负=出账/扣减）
     * @param checkInDate 签到日期；非签到变动传 null
     * @return 变动后的余额
     * @throws com.campushub.common.BusinessException 余额不足 409 {@code INSUFFICIENT_POINTS}；
     *         重复签到 409 {@code ALREADY_CHECKED_IN_TODAY}；并发冲突 409
     *         {@code DUPLICATE_OR_CONFLICTED_OPERATION}；用户不存在 404
     */
    long applyPointChange(Long userId, long delta, java.time.LocalDate checkInDate);
}
