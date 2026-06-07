package com.campushub.service.impl;

import com.campushub.common.BusinessException;
import com.campushub.common.CurrentUserContext;
import com.campushub.dto.user.AdminHandleRequest;
import com.campushub.dto.user.AdminHandleResult;
import com.campushub.dto.user.VerificationSubmitResponse;
import com.campushub.entity.VerificationReview;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.FileBusinessType;
import com.campushub.entity.enums.FileUsage;
import com.campushub.entity.enums.ReviewStatus;
import com.campushub.entity.enums.NotificationType;
import com.campushub.entity.enums.UserRole;
import com.campushub.mapper.VerificationReviewMapper;
import com.campushub.service.FileStorageService;
import com.campushub.service.NotificationService;
import com.campushub.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationReviewServiceImplTest {

    private final VerificationReviewMapper reviewMapper = mock(VerificationReviewMapper.class);
    private final UserService userService = mock(UserService.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final VerificationReviewServiceImpl service =
            new VerificationReviewServiceImpl(reviewMapper, userService, fileStorageService, notificationService);

    @Test
    void submitVerification_createsReviewAndDelegatesUserStatus() {
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        when(userService.isStudentIdCertified(eq("20260001"), eq(7L))).thenReturn(false);
        MockMultipartFile file = new MockMultipartFile("verificationImage", "card.jpg", "image/jpeg", new byte[]{1});
        when(fileStorageService.uploadImage(eq(file), eq(7L), eq(FileUsage.VERIFICATION_MATERIAL),
                eq(FileBusinessType.VERIFICATION_REVIEW), eq(null))).thenReturn(88L);

        VerificationSubmitResponse response = service.submitVerification(7L, "20260001", "张三", file);

        assertThat(response.getAuthStatus()).isEqualTo(AuthStatus.REVIEWING);
        ArgumentCaptor<VerificationReview> reviewCaptor = ArgumentCaptor.forClass(VerificationReview.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        VerificationReview review = reviewCaptor.getValue();
        assertThat(review.getUserId()).isEqualTo(7L);
        assertThat(review.getMaterialFileId()).isEqualTo(88L);
        assertThat(review.getSubmittedStudentId()).isEqualTo("20260001");
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PENDING);
        verify(fileStorageService).updateBusinessTrace(88L, FileBusinessType.VERIFICATION_REVIEW, review.getId());
        verify(userService).markVerificationSubmitted(7L);
    }

    @Test
    void submitVerification_rejectsExistingPendingReview() {
        when(reviewMapper.selectCount(any())).thenReturn(1L);
        MockMultipartFile file = new MockMultipartFile("verificationImage", "card.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> service.submitVerification(7L, "20260001", "张三", file))
                .isInstanceOf(BusinessException.class);
        verify(fileStorageService, never()).uploadImage(any(), any(), any(), any(), any());
        verify(userService, never()).markVerificationSubmitted(any());
    }

    @Test
    void approveReview_delegatesUserUpdateAndMarksReviewApproved() {
        VerificationReview review = pendingReview();
        when(reviewMapper.selectById(1L)).thenReturn(review);
        AdminHandleRequest request = new AdminHandleRequest();
        request.setResult(AdminHandleResult.APPROVE);

        service.handleReview(admin(), 1L, request);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(review.getReviewerId()).isEqualTo(99L);
        verify(userService).markVerificationApproved(7L, "20260001", "张三");
        verify(reviewMapper).updateById(review);
        verify(notificationService).createNotice(eq(7L), eq(NotificationType.VERIFICATION),
                any(), any(), any(), any());
    }

    @Test
    void rejectReview_delegatesUserUpdateAndPersistsReason() {
        VerificationReview review = pendingReview();
        when(reviewMapper.selectById(1L)).thenReturn(review);
        AdminHandleRequest request = new AdminHandleRequest();
        request.setResult(AdminHandleResult.REJECT);
        request.setReason("  材料模糊  ");

        service.handleReview(admin(), 1L, request);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.REJECTED);
        assertThat(review.getReviewerId()).isEqualTo(99L);
        assertThat(review.getRejectReason()).isEqualTo("材料模糊");
        verify(userService).markVerificationRejected(7L);
        verify(reviewMapper).updateById(review);
        verify(notificationService).createNotice(eq(7L), eq(NotificationType.VERIFICATION),
                any(), any(), any(), any());
    }

    @Test
    void nonAdminCannotQueryReviews() {
        assertThatThrownBy(() -> service.queryReviews(
                new CurrentUserContext(7L, UserRole.USER, AuthStatus.APPROVED), null, new com.campushub.common.PageQuery()))
                .isInstanceOf(BusinessException.class);
    }

    private CurrentUserContext admin() {
        return new CurrentUserContext(99L, UserRole.ADMIN, AuthStatus.APPROVED);
    }

    private VerificationReview pendingReview() {
        VerificationReview review = new VerificationReview();
        review.setId(1L);
        review.setUserId(7L);
        review.setSubmittedStudentId("20260001");
        review.setSubmittedRealName("张三");
        review.setStatus(ReviewStatus.PENDING);
        return review;
    }
}
