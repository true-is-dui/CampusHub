package com.campushub.service.impl;

import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.dto.pickup.PickupAcceptResult;
import com.campushub.dto.pickup.PickupCreateRequest;
import com.campushub.dto.pickup.PickupCreateResult;
import com.campushub.entity.PickupRequest;
import com.campushub.entity.enums.FileUsage;
import com.campushub.entity.enums.NotificationType;
import com.campushub.entity.enums.PickupCancelReason;
import com.campushub.entity.enums.PickupStatus;
import com.campushub.entity.enums.RewardType;
import com.campushub.mapper.PickupRequestMapper;
import com.campushub.service.FileStorageService;
import com.campushub.service.NotificationService;
import com.campushub.service.PointService;
import com.campushub.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PickupServiceImpl} 单元测试：mock mapper / UserService / FileStorageService /
 * PointService，验证状态流转、鉴权与有报酬积分扣减/退回/转入分支，不连数据库。
 */
@ExtendWith(MockitoExtension.class)
class PickupServiceImplTest {

    @Mock
    private PickupRequestMapper pickupRequestMapper;
    @Mock
    private UserService userService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private PointService pointService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PickupServiceImpl pickupService;

    private final MultipartFile image =
            new MockMultipartFile("f", "c.jpg", "image/jpeg", new byte[]{1});

    private PickupCreateRequest createRequest(RewardType type, BigDecimal amount) {
        PickupCreateRequest r = new PickupCreateRequest();
        r.setCampus("XIANLIN");
        r.setPickupLocation("A");
        r.setDeliveryLocation("B");
        r.setItemDescription("desc");
        r.setRewardType(type);
        r.setRewardAmount(amount);
        r.setAcceptDeadline(LocalDateTime.now().plusHours(1));
        return r;
    }

    @Test
    void publish_unpaid_goesWaitingAccept_noPointSpend() {
        when(fileStorageService.uploadImage(any(), eq(1L), eq(FileUsage.PICKUP_CREDENTIAL), any(), any()))
                .thenReturn(99L);

        PickupCreateResult result =
                pickupService.publishPickup(1L, createRequest(RewardType.UNPAID, null), image);

        assertThat(result.getStatus()).isEqualTo(PickupStatus.WAITING_ACCEPT);
        verify(userService).ensureCertified(1L);
        verify(pointService, never()).spendForPublish(anyLong(), anyLong(), any());
        verify(pickupRequestMapper).insert(any(PickupRequest.class));
        // 发布成功通知发布方自己（PICKUP）。
        verify(notificationService).createNotice(eq(1L), eq(NotificationType.PICKUP),
                anyString(), anyString(), any(), any());
    }

    @Test
    void publish_paid_goesWaitingAccept_spendsPoints() {
        when(fileStorageService.uploadImage(any(), anyLong(), any(), any(), any())).thenReturn(99L);

        PickupCreateResult result =
                pickupService.publishPickup(1L, createRequest(RewardType.PAID, new BigDecimal("10")), image);

        assertThat(result.getStatus()).isEqualTo(PickupStatus.WAITING_ACCEPT);
        // 有报酬发布直接进入待接单，并扣减发布方 10 积分（金额→积分）。
        verify(pointService).spendForPublish(eq(1L), eq(10L), any());
    }

    @Test
    void publish_paid_insufficientPoints_propagates409() {
        when(fileStorageService.uploadImage(any(), anyLong(), any(), any(), any())).thenReturn(99L);
        doThrow(new BusinessException(ErrorCode.CONFLICT,
                com.campushub.common.ErrorReason.INSUFFICIENT_POINTS))
                .when(pointService).spendForPublish(anyLong(), anyLong(), any());

        assertThatThrownBy(() ->
                pickupService.publishPickup(1L, createRequest(RewardType.PAID, new BigDecimal("10")), image))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void accept_rejectsPublisherAcceptingOwn() {
        PickupRequest p = waitingAccept(10L, 1L);
        when(pickupRequestMapper.selectById(10L)).thenReturn(p);

        assertThatThrownBy(() -> pickupService.acceptPickup(10L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void accept_expired_cancelsAndThrows409() {
        PickupRequest p = waitingAccept(10L, 1L);
        p.setAcceptDeadline(LocalDateTime.now().minusMinutes(1));
        when(pickupRequestMapper.selectById(10L)).thenReturn(p);
        when(pickupRequestMapper.update(any(), any())).thenReturn(1);

        assertThatThrownBy(() -> pickupService.acceptPickup(10L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        assertThat(p.getStatus()).isEqualTo(PickupStatus.CANCELLED);
        assertThat(p.getCancelReason()).isEqualTo(PickupCancelReason.ACCEPT_DEADLINE_EXPIRED);
    }

    @Test
    void accept_success_goesInProgress() {
        PickupRequest p = waitingAccept(10L, 1L);
        when(pickupRequestMapper.selectById(10L)).thenReturn(p);
        when(pickupRequestMapper.update(any(), any())).thenReturn(1);

        PickupAcceptResult result = pickupService.acceptPickup(10L, 2L);

        assertThat(result.getStatus()).isEqualTo(PickupStatus.IN_PROGRESS);
        assertThat(p.getAcceptorId()).isEqualTo(2L);
        // 接单成功应通知发布方（publisherId=1L）。
        verify(notificationService).createNotice(eq(1L), eq(NotificationType.PICKUP),
                anyString(), anyString(), any(), eq(10L));
    }

    @Test
    void confirmComplete_rejectsWhenNoProof() {
        PickupRequest p = waitingAccept(10L, 1L);
        p.setStatus(PickupStatus.IN_PROGRESS);
        p.setAcceptorId(2L);
        when(pickupRequestMapper.selectById(10L)).thenReturn(p);

        assertThatThrownBy(() -> pickupService.confirmComplete(10L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void confirmComplete_paid_transfersPointsToAcceptor() {
        PickupRequest p = waitingAccept(10L, 1L);
        p.setStatus(PickupStatus.IN_PROGRESS);
        p.setAcceptorId(2L);
        p.setRewardType(RewardType.PAID);
        p.setRewardAmount(new BigDecimal("10"));
        p.setCompletionProofFileId(77L);
        when(pickupRequestMapper.selectById(10L)).thenReturn(p);
        when(pickupRequestMapper.update(any(), any())).thenReturn(1);

        var result = pickupService.confirmComplete(10L, 1L);

        assertThat(result.getStatus()).isEqualTo(PickupStatus.COMPLETED);
        // 完成把报酬积分转入接单方（发布方 1L、接单方 2L、10 积分）。
        verify(pointService).transferOnComplete(eq(1L), eq(2L), eq(10L), eq(10L));
        // 有报酬完成：接单方收到独立的入账通知（PAYMENT）+ 确认完成通知（PICKUP）。
        verify(notificationService).createNotice(eq(2L), eq(NotificationType.PAYMENT),
                anyString(), anyString(), any(), eq(10L));
        verify(notificationService).createNotice(eq(2L), eq(NotificationType.PICKUP),
                anyString(), anyString(), any(), eq(10L));
    }

    @Test
    void confirmComplete_unpaid_noPaymentNotice() {
        PickupRequest p = waitingAccept(10L, 1L);
        p.setStatus(PickupStatus.IN_PROGRESS);
        p.setAcceptorId(2L);
        p.setRewardType(RewardType.UNPAID);
        p.setRewardAmount(null);
        p.setCompletionProofFileId(77L);
        when(pickupRequestMapper.selectById(10L)).thenReturn(p);
        when(pickupRequestMapper.update(any(), any())).thenReturn(1);

        var result = pickupService.confirmComplete(10L, 1L);

        assertThat(result.getStatus()).isEqualTo(PickupStatus.COMPLETED);
        // 无报酬不转积分、不发入账通知；仍发确认完成通知（PICKUP）。
        verify(pointService, never()).transferOnComplete(anyLong(), anyLong(), anyLong(), any());
        verify(notificationService, never()).createNotice(any(), eq(NotificationType.PAYMENT),
                any(), any(), any(), any());
        verify(notificationService).createNotice(eq(2L), eq(NotificationType.PICKUP),
                anyString(), anyString(), any(), eq(10L));
    }

    @Test
    void cancel_inProgress_isRejected() {
        PickupRequest p = waitingAccept(10L, 1L);
        p.setStatus(PickupStatus.IN_PROGRESS);
        when(pickupRequestMapper.selectById(10L)).thenReturn(p);

        assertThatThrownBy(() -> pickupService.cancelPickup(10L, 1L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void cancel_waitingAccept_paid_refundsPoints() {
        PickupRequest p = waitingAccept(10L, 1L);
        p.setStatus(PickupStatus.WAITING_ACCEPT);
        p.setRewardType(RewardType.PAID);
        p.setRewardAmount(new BigDecimal("10"));
        when(pickupRequestMapper.selectById(10L)).thenReturn(p);
        when(pickupRequestMapper.update(any(), any())).thenReturn(1);

        var result = pickupService.cancelPickup(10L, 1L, "改主意了");

        assertThat(result.getStatus()).isEqualTo(PickupStatus.CANCELLED);
        // 待接单阶段取消有报酬服务，退回发布方 10 积分。
        verify(pointService).refundForCancel(eq(1L), eq(10L), eq(10L));
    }

    @Test
    void cancel_waitingAccept_unpaid_noPointOps() {
        PickupRequest p = waitingAccept(10L, 1L);
        when(pickupRequestMapper.selectById(10L)).thenReturn(p);
        when(pickupRequestMapper.update(any(), any())).thenReturn(1);

        var result = pickupService.cancelPickup(10L, 1L, null);

        assertThat(result.getStatus()).isEqualTo(PickupStatus.CANCELLED);
        verify(pointService, never()).refundForCancel(anyLong(), anyLong(), any());
    }

    @Test
    void loadCredential_rejectsNonParticipant() {
        PickupRequest p = waitingAccept(10L, 1L);
        when(pickupRequestMapper.selectById(10L)).thenReturn(p);

        assertThatThrownBy(() -> pickupService.loadPickupCredential(10L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    /** 构造一个待接单服务（id, publisherId）。 */
    private PickupRequest waitingAccept(Long id, Long publisherId) {
        PickupRequest p = new PickupRequest();
        p.setId(id);
        p.setPublisherId(publisherId);
        p.setStatus(PickupStatus.WAITING_ACCEPT);
        p.setRewardType(RewardType.UNPAID);
        p.setAcceptDeadline(LocalDateTime.now().plusHours(1));
        p.setPickupCredentialFileId(88L);
        return p;
    }
}
