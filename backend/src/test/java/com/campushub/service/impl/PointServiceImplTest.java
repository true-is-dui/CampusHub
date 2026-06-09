package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.point.CheckInResult;
import com.campushub.dto.point.PointTransactionItem;
import com.campushub.entity.PointTransaction;
import com.campushub.entity.User;
import com.campushub.entity.enums.PointTransactionType;
import com.campushub.mapper.PointTransactionMapper;
import com.campushub.mapper.UserMapper;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PointServiceImpl} 单元测试：mock UserMapper / PointTransactionMapper，验证积分变动的
 * 余额更新 + 流水写入一致性、签到去重、发布扣减的余额校验，不连数据库。
 */
@ExtendWith(MockitoExtension.class)
class PointServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PointTransactionMapper pointTransactionMapper;

    @InjectMocks
    private PointServiceImpl pointService;

    private User user(Long id, long balance) {
        User u = new User();
        u.setId(id);
        u.setPointBalance(balance);
        return u;
    }

    @Test
    void grantInitialPoints_adds100_writesVerificationTx() {
        when(userMapper.selectById(7L)).thenReturn(user(7L, 0L));
        when(userMapper.update(any(), any())).thenReturn(1);

        pointService.grantInitialPoints(7L);

        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionMapper).insert(captor.capture());
        PointTransaction tx = captor.getValue();
        assertThat(tx.getType()).isEqualTo(PointTransactionType.EARN_VERIFICATION);
        assertThat(tx.getAmount()).isEqualTo(100L);
        assertThat(tx.getBalanceAfter()).isEqualTo(100L);
        assertThat(tx.getRelatedPickupId()).isNull();
    }

    @Test
    void checkIn_firstToday_adds5_andReturnsResult() {
        when(userMapper.selectById(7L)).thenReturn(user(7L, 10L));
        when(userMapper.update(any(), any())).thenReturn(1);

        CheckInResult result = pointService.checkIn(7L);

        assertThat(result.getEarnedPoints()).isEqualTo(5);
        assertThat(result.getPointBalance()).isEqualTo(15L);
        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionMapper).insert(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(PointTransactionType.EARN_CHECK_IN);
        assertThat(captor.getValue().getBalanceAfter()).isEqualTo(15L);
    }

    @Test
    void checkIn_alreadyToday_throws409_noTx() {
        User u = user(7L, 10L);
        u.setLastCheckInDate(LocalDate.now());
        when(userMapper.selectById(7L)).thenReturn(u);

        assertThatThrownBy(() -> pointService.checkIn(7L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(pointTransactionMapper, never()).insert(any(PointTransaction.class));
    }

    @Test
    void spendForPublish_sufficient_deductsAndWritesNegativeTx() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, 50L));
        when(userMapper.update(any(), any())).thenReturn(1);

        pointService.spendForPublish(2L, 10L, 100L);

        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionMapper).insert(captor.capture());
        PointTransaction tx = captor.getValue();
        assertThat(tx.getType()).isEqualTo(PointTransactionType.SPEND_PUBLISH);
        assertThat(tx.getAmount()).isEqualTo(-10L);
        assertThat(tx.getBalanceAfter()).isEqualTo(40L);
        assertThat(tx.getRelatedPickupId()).isEqualTo(100L);
    }

    @Test
    void spendForPublish_insufficient_throws409_noUpdateNoTx() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, 5L));

        assertThatThrownBy(() -> pointService.spendForPublish(2L, 10L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(userMapper, never()).update(any(), any());
        verify(pointTransactionMapper, never()).insert(any(PointTransaction.class));
    }

    @Test
    void refundForCancel_addsBack_writesRefundTx() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, 40L));
        when(userMapper.update(any(), any())).thenReturn(1);

        pointService.refundForCancel(2L, 10L, 100L);

        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionMapper).insert(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(PointTransactionType.REFUND_CANCEL);
        assertThat(captor.getValue().getAmount()).isEqualTo(10L);
        assertThat(captor.getValue().getBalanceAfter()).isEqualTo(50L);
    }

    @Test
    void transferOnComplete_creditsReceiverOnly() {
        when(userMapper.selectById(3L)).thenReturn(user(3L, 0L));
        when(userMapper.update(any(), any())).thenReturn(1);

        pointService.transferOnComplete(2L, 3L, 10L, 100L);

        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionMapper).insert(captor.capture());
        PointTransaction tx = captor.getValue();
        assertThat(tx.getUserId()).isEqualTo(3L);
        assertThat(tx.getType()).isEqualTo(PointTransactionType.INCOME_COMPLETE);
        assertThat(tx.getAmount()).isEqualTo(10L);
        assertThat(tx.getBalanceAfter()).isEqualTo(10L);
        // 只对接单方入账，不查询/更新发布方。
        verify(userMapper).selectById(3L);
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
}
