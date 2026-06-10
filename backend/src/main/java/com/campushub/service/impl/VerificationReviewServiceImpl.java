package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.BusinessException;
import com.campushub.common.CurrentUserContext;
import com.campushub.common.ErrorCode;
import com.campushub.common.ErrorReason;
import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.user.AdminHandleRequest;
import com.campushub.dto.user.AdminHandleResult;
import com.campushub.dto.user.VerificationReviewSummary;
import com.campushub.dto.user.VerificationSubmitResponse;
import com.campushub.entity.VerificationReview;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.FileBusinessType;
import com.campushub.entity.enums.FileUsage;
import com.campushub.entity.enums.NotificationType;
import com.campushub.entity.enums.ReviewStatus;
import com.campushub.mapper.VerificationReviewMapper;
import com.campushub.service.FileStorageService;
import com.campushub.service.NotificationService;
import com.campushub.service.PointService;
import com.campushub.service.UserService;
import com.campushub.service.VerificationReviewService;
import com.campushub.service.dto.StoredFileContent;
import com.campushub.service.dto.UserBrief;
import com.campushub.util.StudentIdMasker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 实名认证审核服务实现：编排提交与审核流转。
 *
 * <p>verification_reviews 记录归本服务读写；用户认证状态变更委托 {@link UserService}
 * 的状态变更原语完成（依赖方向单向：本服务 → UserService，无循环依赖）。
 */
@Service
@RequiredArgsConstructor
public class VerificationReviewServiceImpl implements VerificationReviewService {

    private final VerificationReviewMapper verificationReviewMapper;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final PointService pointService;

    @Override
    @Transactional
    public VerificationSubmitResponse submitVerification(Long userId, String studentId, String realName,
                                                         MultipartFile verificationImage) {
        String normalizedStudentId = studentId == null ? null : studentId.trim();
        String normalizedRealName = realName == null ? null : realName.trim();

        // 已有待审核申请则拒绝重复提交（review 记录归本模块）
        Long pendingCount = verificationReviewMapper.selectCount(Wrappers.<VerificationReview>lambdaQuery()
                .eq(VerificationReview::getUserId, userId)
                .eq(VerificationReview::getStatus, ReviewStatus.PENDING));
        if (pendingCount != null && pendingCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    ErrorReason.DUPLICATE_OR_CONFLICTED_OPERATION, "已有待审核的实名认证申请");
        }
        // 学号唯一性预检（用户表查询经 UserService）
        if (userService.isStudentIdCertified(normalizedStudentId, userId)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    ErrorReason.DUPLICATE_OR_CONFLICTED_OPERATION, "该学号已被认证");
        }

        Long materialFileId = fileStorageService.uploadImage(
                verificationImage, userId, FileUsage.VERIFICATION_MATERIAL,
                FileBusinessType.VERIFICATION_REVIEW, null);

        VerificationReview review = new VerificationReview();
        review.setUserId(userId);
        review.setMaterialFileId(materialFileId);
        review.setSubmittedStudentId(normalizedStudentId);
        review.setSubmittedRealName(normalizedRealName);
        review.setStatus(ReviewStatus.PENDING);
        verificationReviewMapper.insert(review);
        fileStorageService.updateBusinessTrace(materialFileId,
                FileBusinessType.VERIFICATION_REVIEW, review.getId());

        // 用户认证状态置为审核中（含「已通过不能重复提交」校验），归口 UserService
        userService.markVerificationSubmitted(userId);
        notificationService.createNotice(userId, NotificationType.VERIFICATION,
                "实名认证已提交", "您的实名认证申请已提交，请等待管理员审核。", null, null);
        return new VerificationSubmitResponse(AuthStatus.REVIEWING);
    }

    @Override
    public PageResult<VerificationReviewSummary> queryReviews(CurrentUserContext admin,
                                                              ReviewStatus status,
                                                              PageQuery pageQuery) {
        requireAdmin(admin);
        IPage<VerificationReview> page = verificationReviewMapper.selectPage(
                pageQuery.toMpPage(),
                Wrappers.<VerificationReview>lambdaQuery()
                        .eq(status != null, VerificationReview::getStatus, status)
                        .orderByDesc(VerificationReview::getCreatedAt));
        Map<Long, UserBrief> users = loadUsers(page.getRecords());
        List<VerificationReviewSummary> list = page.getRecords().stream()
                .map(review -> toSummary(review, users.get(review.getUserId())))
                .toList();
        return PageResult.of(page, list);
    }

    @Override
    @Transactional
    public void handleReview(CurrentUserContext admin, Long reviewId, AdminHandleRequest request) {
        requireAdmin(admin);
        VerificationReview review = requireReview(reviewId);
        if (!review.isPending()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    ErrorReason.VERIFICATION_REVIEW_ALREADY_HANDLED);
        }
        if (request.getResult() == AdminHandleResult.APPROVE) {
            approve(review, admin.getCurrentUserId());
        } else {
            reject(review, admin.getCurrentUserId(), request.getReason());
        }
    }

    @Override
    public StoredFileContent loadReviewImage(CurrentUserContext admin, Long reviewId) {
        requireAdmin(admin);
        VerificationReview review = requireReview(reviewId);
        return fileStorageService.loadFile(review.getMaterialFileId());
    }

    private void approve(VerificationReview review, Long adminId) {
        // 用户状态变更与学号唯一性判定归口 UserService；本模块只负责审核记录本身。
        userService.markVerificationApproved(review.getUserId(),
                review.getSubmittedStudentId(), review.getSubmittedRealName());
        review.markApproved(adminId);
        verificationReviewMapper.updateById(review);
        // 认证通过赠送初始积分（每学号一次：审核通过本身每学号唯一，故此处一次性赠送）。
        pointService.grantInitialPoints(review.getUserId());
        notificationService.createNotice(review.getUserId(), NotificationType.VERIFICATION,
                "实名认证已通过", "您的实名认证申请已通过审核，现在可以发布和接单代取服务。",
                null, null);
    }

    private void reject(VerificationReview review, Long adminId, String reason) {
        // 驳回原因必填已由 AdminHandleRequest 的声明式校验保证，此处仅做规范化。
        String normalizedReason = reason.trim();
        userService.markVerificationRejected(review.getUserId());
        review.markRejected(adminId, normalizedReason);
        verificationReviewMapper.updateById(review);
        notificationService.createNotice(review.getUserId(), NotificationType.VERIFICATION,
                "实名认证未通过", "您的实名认证申请未通过审核，原因：" + normalizedReason + "。可修改后重新提交。",
                null, null);
    }

    private void requireAdmin(CurrentUserContext admin) {
        if (admin == null || !admin.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, ErrorReason.ADMIN_REQUIRED);
        }
    }

    private VerificationReview requireReview(Long reviewId) {
        VerificationReview review = verificationReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ErrorReason.RESOURCE_NOT_FOUND,
                    "审核记录不存在");
        }
        return review;
    }

    private Map<Long, UserBrief> loadUsers(List<VerificationReview> reviews) {
        List<Long> userIds = reviews.stream()
                .map(VerificationReview::getUserId)
                .distinct()
                .toList();
        return userService.getUserBriefs(userIds).stream()
                .collect(Collectors.toMap(UserBrief::getUserId, Function.identity()));
    }

    private VerificationReviewSummary toSummary(VerificationReview review, UserBrief user) {
        return VerificationReviewSummary.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .username(user == null ? null : user.getUsername())
                .nickname(user == null ? null : user.getNickname())
                .studentIdMasked(StudentIdMasker.mask(review.getSubmittedStudentId()))
                .realName(review.getSubmittedRealName())
                .status(review.getStatus())
                .rejectReason(review.getRejectReason())
                .reviewerId(review.getReviewerId())
                .createdAt(review.getCreatedAt())
                .reviewedAt(review.getReviewedAt())
                .build();
    }
}
