package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.CurrentUserContext;
import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.notification.NotificationItem;
import com.campushub.dto.notification.UnreadCountResponse;
import com.campushub.dto.evaluation.EvaluationHistorySummary;
import com.campushub.dto.evaluation.RatingSummary;
import com.campushub.dto.pickup.PickupSummary;
import com.campushub.dto.point.CheckInResult;
import com.campushub.dto.point.PointBalanceResponse;
import com.campushub.dto.point.PointTransactionItem;
import com.campushub.dto.user.UpdateProfileRequest;
import com.campushub.dto.user.UserMeResponse;
import com.campushub.dto.user.UserPublicProfile;
import com.campushub.dto.user.VerificationSubmitRequest;
import com.campushub.dto.user.VerificationSubmitResponse;
import com.campushub.entity.enums.PickupParticipantRole;
import com.campushub.entity.enums.PickupStatus;
import com.campushub.entity.enums.PointTransactionType;
import com.campushub.security.CurrentUser;
import com.campushub.service.PickupService;
import com.campushub.service.dto.StoredFileContent;
import com.campushub.service.EvaluationService;
import com.campushub.service.NotificationService;
import com.campushub.service.PointService;
import com.campushub.service.UserService;
import com.campushub.service.VerificationReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 当前用户接口（{@code /users/**}）。需登录访问，身份由 {@code @CurrentUser} 从 JWT 解析注入，
 * 不接受前端传入的用户标识。
 *
 * <p>controller 按 URL 路径前缀划分：凡 {@code /users/**} 路径的接口都在本类，跨业务模块的接口
 * 由本类<b>委托</b>给对应 owner service（{@code /users/me/verification} → 实名审核服务，
 * {@code /users/me/pickup-requests} → 代取服务），业务逻辑不落在 controller。
 *
 * <p>multipart 表单的文本字段封装为请求 DTO，用 {@code @Valid @ModelAttribute} 在入口
 * 做声明式格式校验（与注册/登录一致）；文件作为独立 {@code @RequestPart} 接收。
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final VerificationReviewService verificationReviewService;
    private final PickupService pickupService;
    private final NotificationService notificationService;
    private final EvaluationService evaluationService;
    private final PointService pointService;


    /** 获取当前登录用户的完整资料（读库返回最新值）。 */
    @GetMapping("/me")
    public ApiResponse<UserMeResponse> getCurrentUser(@CurrentUser CurrentUserContext me) {
        return ApiResponse.ok(userService.getCurrentUser(me.getCurrentUserId()));
    }

    /** 编辑个人资料，支持部分更新和可选头像上传。 */
    @PutMapping(value = "/me/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> updateProfile(@CurrentUser CurrentUserContext me,
                                           @Valid @ModelAttribute UpdateProfileRequest request,
                                           @RequestPart(required = false) MultipartFile avatar) {
        userService.updateProfile(me.getCurrentUserId(), request.getNickname(), avatar,
                request.getCollege(), request.getContact());
        return ApiResponse.ok();
    }

    /** 提交实名认证申请。 */
    @PostMapping(value = "/me/verification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<VerificationSubmitResponse> submitVerification(@CurrentUser CurrentUserContext me,
                                                                      @Valid @ModelAttribute VerificationSubmitRequest request,
                                                                      @RequestPart MultipartFile verificationImage) {
        return ApiResponse.ok(verificationReviewService.submitVerification(
                me.getCurrentUserId(), request.getStudentId(), request.getRealName(), verificationImage));
    }

    /**
     * 查询当前用户参与的代取服务（role=PUBLISHER / ACCEPTOR）。
     *
     * <p>路径在 {@code /users/me} 下，故归本 controller；业务委托给 {@link PickupService}
     * （与 {@code /users/me/verification} 委托实名审核服务同模式）。
     */
    @GetMapping("/me/pickup-requests")
    public ApiResponse<PageResult<PickupSummary>> myPickups(
            @CurrentUser CurrentUserContext me,
            @RequestParam PickupParticipantRole role,
            @RequestParam(required = false) PickupStatus status,
            @Valid PageQuery pageQuery) {
        Long userId = me.getCurrentUserId();
        PageResult<PickupSummary> result = role == PickupParticipantRole.PUBLISHER
                ? pickupService.queryMyPublished(userId, status, pageQuery)
                : pickupService.queryMyAccepted(userId, status, pageQuery);
        return ApiResponse.ok(result);
    }

    /**
     * 查询当前用户的站内通知列表（分页，按创建时间倒序）。
     *
     * <p>路径在 {@code /users/me} 下，归本 controller；委托给 {@link NotificationService}。
     */
    @GetMapping("/me/notifications")
    public ApiResponse<PageResult<NotificationItem>> myNotifications(
            @CurrentUser CurrentUserContext me,
            @Valid PageQuery pageQuery) {
        return ApiResponse.ok(notificationService.queryMyNotices(me.getCurrentUserId(), pageQuery));
    }

    /** 查询当前用户的未读通知数量（供前端低频轮询）。 */
    @GetMapping("/me/notifications/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(@CurrentUser CurrentUserContext me) {
        long count = notificationService.countUnread(me.getCurrentUserId());
        return ApiResponse.ok(new UnreadCountResponse(count));
    }

    /** 将指定通知标记为已读（仅接收者本人可操作）。 */
    @PostMapping("/me/notifications/{notificationId}/read")
    public ApiResponse<Void> markNotificationRead(@CurrentUser CurrentUserContext me,
                                                  @PathVariable Long notificationId) {
        notificationService.markRead(notificationId, me.getCurrentUserId());
        return ApiResponse.ok();
    }

    /** 查询当前用户的平台积分余额。委托 {@link PointService}。 */
    @GetMapping("/me/point-balance")
    public ApiResponse<PointBalanceResponse> pointBalance(@CurrentUser CurrentUserContext me) {
        long balance = pointService.getBalance(me.getCurrentUserId());
        return ApiResponse.ok(new PointBalanceResponse(balance));
    }

    /** 每日签到领取积分（每日一次，重复签到返回 409）。委托 {@link PointService}。 */
    @PostMapping("/me/check-in")
    public ApiResponse<CheckInResult> checkIn(@CurrentUser CurrentUserContext me) {
        return ApiResponse.ok(pointService.checkIn(me.getCurrentUserId()));
    }

    /**
     * 查询当前用户的积分流水（分页，按创建时间倒序，可按 {@code type} 筛选）。
     * 委托 {@link PointService}。
     */
    @GetMapping("/me/point-transactions")
    public ApiResponse<PageResult<PointTransactionItem>> pointTransactions(
            @CurrentUser CurrentUserContext me,
            @RequestParam(required = false) PointTransactionType type,
            @Valid PageQuery pageQuery) {
        return ApiResponse.ok(pointService.queryTransactions(me.getCurrentUserId(), type, pageQuery));
    }

    /** 公开读取用户头像。 */
    @GetMapping("/{userId}/avatar")
    public ResponseEntity<Resource> getAvatar(@PathVariable Long userId) {
        StoredFileContent content = userService.loadAvatar(userId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(content.getMimeType()))
            .contentLength(content.getFileSize())
            .body(content.getResource());
    }

    /**
     * 查看用户公开主页（公开访问）。{@code includeRating=true} 时附带评价摘要，否则为空。
     *
     * <p>公开资料经 {@link UserService}；评价摘要按需经 {@link EvaluationService} 填充，
     * 保持用户模块不反向依赖评价模块（组合在 controller 完成）。
     */
    @GetMapping("/{userId}/profile")
    public ApiResponse<UserPublicProfile> publicProfile(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "false") boolean includeRating) {
        UserPublicProfile profile = userService.getPublicProfile(userId);
        if (includeRating) {
            RatingSummary ratingSummary = evaluationService.queryUserRatingSummary(userId);
            profile = UserPublicProfile.builder()
                    .nickname(profile.getNickname())
                    .college(profile.getCollege())
                    .contact(profile.getContact())
                    .ratingSummary(ratingSummary)
                    .build();
        }
        return ApiResponse.ok(profile);
    }

    /**
     * 查询用户好评率摘要（公开访问）。路径在 {@code /users/{userId}} 下，归本 controller；
     * 委托 {@link EvaluationService}。
     */
    @GetMapping("/{userId}/rating-summary")
    public ApiResponse<RatingSummary> ratingSummary(@PathVariable Long userId) {
        return ApiResponse.ok(evaluationService.queryUserRatingSummary(userId));
    }

    /** 查询用户收到的评价列表（公开访问，分页）。委托 {@link EvaluationService}。 */
    @GetMapping("/{userId}/evaluations")
    public ApiResponse<PageResult<EvaluationHistorySummary>> userEvaluations(
            @PathVariable Long userId,
            @Valid PageQuery pageQuery) {
        return ApiResponse.ok(evaluationService.queryUserEvaluations(userId, pageQuery));
    }
}
