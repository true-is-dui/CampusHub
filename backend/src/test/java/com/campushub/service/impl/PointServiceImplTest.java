package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.ErrorReason;
import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.point.CheckInResult;
import com.campushub.dto.point.PointTransactionItem;
import com.campushub.entity.PointTransaction;
import com.campushub.entity.enums.PointTransactionType;
import com.campushub.mapper.PointTransactionMapper;
import com.campushub.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PointServiceImpl} 单元测试：mock {@link UserService}（积分变动原语）+
 * {@link PointTransactionMapper}，验证「积分业务编排 + 写流水」职责——余额变动委托
 * UserService.applyPointChange、据其返回的 balanceAfter 写正确类型/符号的流水；
 * 余额不足/重复签到的 409 由 applyPointChange 抛出、PointService 不再读用户表。不连数据库。
 */
@ExtendWith(MockitoExtension.class)
class PointServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private PointTransactionMapper pointTransactionMapper;

    @InjectMocks
    private PointServiceImpl pointService;

    @Test
    void grantInitialPoints_adds100_writesVerificationTx() {
        when(userService.applyPointChange(7L, 100L, null)).thenReturn(100L);

        pointService.grantInitialPoints(7L);

        PointTransaction tx = captureTx();
        assertThat(tx.getType()).isEqualTo(PointTransactionType.EARN_VERIFICATION);
        assertThat(tx.getAmount()).isEqualTo(100L);
        assertThat(tx.getBalanceAfter()).isEqualTo(100L);
        assertThat(tx.getRelatedPickupId()).isNull();
    }

    @Test
    void checkIn_firstToday_adds5_andReturnsResult() {
        // 签到变动：applyPointChange 收到非 null 的签到日期。
        when(userService.applyPointChange(eq(7L), eq(5L), any(LocalDate.class))).thenReturn(15L);

        CheckInResult result = pointService.checkIn(7L);

        assertThat(result.getEarnedPoints()).isEqualTo(5);
        assertThat(result.getPointBalance()).isEqualTo(15L);
        PointTransaction tx = captureTx();
        assertThat(tx.getType()).isEqualTo(PointTransactionType.EARN_CHECK_IN);
        assertThat(tx.getBalanceAfter()).isEqualTo(15L);
    }

    @Test
    void checkIn_alreadyToday_throws409_noTx() {
        when(userService.applyPointChange(eq(7L), eq(5L), any(LocalDate.class)))
                .thenThrow(new BusinessException(ErrorCode.CONFLICT, ErrorReason.ALREADY_CHECKED_IN_TODAY));

        assertThatThrownBy(() -> pointService.checkIn(7L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(pointTransactionMapper, never()).insert(any(PointTransaction.class));
    }

    @Test
    void getBalance_delegatesToUserService() {
        when(userService.getPointBalance(7L)).thenReturn(42L);

        assertThat(pointService.getBalance(7L)).isEqualTo(42L);
    }

    @Test
    void spendForPublish_sufficient_deductsAndWritesNegativeTx() {
        when(userService.applyPointChange(2L, -10L, null)).thenReturn(40L);

        pointService.spendForPublish(2L, 10L, 100L);

        PointTransaction tx = captureTx();
        assertThat(tx.getType()).isEqualTo(PointTransactionType.SPEND_PUBLISH);
        assertThat(tx.getAmount()).isEqualTo(-10L);
        assertThat(tx.getBalanceAfter()).isEqualTo(40L);
        assertThat(tx.getRelatedPickupId()).isEqualTo(100L);
    }

    @Test
    void spendForPublish_insufficient_throws409_noTx() {
        when(userService.applyPointChange(2L, -10L, null))
                .thenThrow(new BusinessException(ErrorCode.CONFLICT, ErrorReason.INSUFFICIENT_POINTS));

        assertThatThrownBy(() -> pointService.spendForPublish(2L, 10L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        // 扣减失败不写流水。
        verify(pointTransactionMapper, never()).insert(any(PointTransaction.class));
    }

    @Test
    void refundForCancel_addsBack_writesRefundTx() {
        when(userService.applyPointChange(2L, 10L, null)).thenReturn(50L);

        pointService.refundForCancel(2L, 10L, 100L);

        PointTransaction tx = captureTx();
        assertThat(tx.getType()).isEqualTo(PointTransactionType.REFUND_CANCEL);
        assertThat(tx.getAmount()).isEqualTo(10L);
        assertThat(tx.getBalanceAfter()).isEqualTo(50L);
        assertThat(tx.getRelatedPickupId()).isEqualTo(100L);
    }

    @Test
    void transferOnComplete_creditsReceiverOnly() {
        when(userService.applyPointChange(3L, 10L, null)).thenReturn(10L);

        pointService.transferOnComplete(2L, 3L, 10L, 100L);

        PointTransaction tx = captureTx();
        assertThat(tx.getUserId()).isEqualTo(3L);
        assertThat(tx.getType()).isEqualTo(PointTransactionType.INCOME_COMPLETE);
        assertThat(tx.getAmount()).isEqualTo(10L);
        assertThat(tx.getBalanceAfter()).isEqualTo(10L);
        // 只对接单方(3L)入账，不动发布方(2L)。
        verify(userService, never()).applyPointChange(eq(2L), anyLong(), isNull());
    }

    @Test
    void queryTransactions_mapsToVo() {
        PointTransaction tx = new PointTransaction();
        tx.setId(1L);
        tx.setUserId(7L);
        tx.setType(PointTransactionType.EARN_VERIFICATION);
        tx.setAmount(100L);
        tx.setBalanceAfter(100L);
        tx.setCreatedAt(LocalDateTime.now());
        Page<PointTransaction> page = new Page<>(1, 20);
        page.setRecords(List.of(tx));
        page.setTotal(1);
        when(pointTransactionMapper.selectPage(any(IPage.class), any())).thenReturn(page);

        PageResult<PointTransactionItem> result =
                pointService.queryTransactions(7L, null, new PageQuery());

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getType()).isEqualTo(PointTransactionType.EARN_VERIFICATION);
        assertThat(result.getList().get(0).getBalanceAfter()).isEqualTo(100L);
    }

    private PointTransaction captureTx() {
        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionMapper).insert(captor.capture());
        return captor.getValue();
    }
}
