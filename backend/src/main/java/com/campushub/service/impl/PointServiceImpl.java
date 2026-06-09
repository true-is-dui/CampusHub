package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.point.CheckInResult;
import com.campushub.dto.point.PointTransactionItem;
import com.campushub.entity.PointTransaction;
import com.campushub.entity.enums.PointTransactionType;
import com.campushub.mapper.PointTransactionMapper;
import com.campushub.service.PointService;
import com.campushub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * {@link PointService} 实现。
 *
 * <p>职责 = <b>积分业务编排 + 写流水</b>：判定业务规则（赠送/签到/发布扣减/取消退回/完成入账），
 * 调 {@link UserService#applyPointChange} 完成「读用户→改余额→条件落库」并拿到 balanceAfter，
 * 再写一条 {@code point_transactions} 流水。两步在同一事务内（本方法 {@code @Transactional}，
 * {@code applyPointChange} 经 REQUIRED 并入），保证「余额与流水」一致。
 *
 * <p><b>用户表读写收口 {@link UserService}（owner service）</b>：本类不注入 {@code UserMapper}，
 * 余额条件更新、签到去重、余额不足判定均在 {@code UserService.applyPointChange} 内（CLAUDE.md
 * 第四批准则「用户表读取/变更经 owner service」）。本类只持 {@link PointTransactionMapper}
 * （积分流水是本模块自有表）。
 */
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    /** 实名认证通过赠送积分（当前 MVP）。 */
    private static final long INITIAL_POINTS = 100L;
    /** 每日签到赠送积分（当前 MVP）。 */
    private static final int CHECK_IN_POINTS = 5;

    private final UserService userService;
    private final PointTransactionMapper pointTransactionMapper;

    @Override
    @Transactional
    public void grantInitialPoints(Long userId) {
        long balanceAfter = userService.applyPointChange(userId, INITIAL_POINTS, null);
        writeTransaction(userId, PointTransactionType.EARN_VERIFICATION, INITIAL_POINTS, balanceAfter, null);
    }

    @Override
    @Transactional
    public CheckInResult checkIn(Long userId) {
        // 传 today 即「签到变动」语义：余额 +5 与签到日期同笔落库，当日重复签到由 owner service
        // 的「旧签到日期」条件拦截抛 409 ALREADY_CHECKED_IN_TODAY。
        long balanceAfter = userService.applyPointChange(userId, CHECK_IN_POINTS, LocalDate.now());
        writeTransaction(userId, PointTransactionType.EARN_CHECK_IN, CHECK_IN_POINTS, balanceAfter, null);
        return CheckInResult.builder()
                .earnedPoints(CHECK_IN_POINTS)
                .pointBalance(balanceAfter)
                .build();
    }

    @Override
    public long getBalance(Long userId) {
        return userService.getPointBalance(userId);
    }

    @Override
    @Transactional
    public void spendForPublish(Long userId, long amount, Long pickupId) {
        // 余额不足由 applyPointChange 抛 409 INSUFFICIENT_POINTS（不写流水）。
        long balanceAfter = userService.applyPointChange(userId, -amount, null);
        writeTransaction(userId, PointTransactionType.SPEND_PUBLISH, -amount, balanceAfter, pickupId);
    }

    @Override
    @Transactional
    public void refundForCancel(Long userId, long amount, Long pickupId) {
        long balanceAfter = userService.applyPointChange(userId, amount, null);
        writeTransaction(userId, PointTransactionType.REFUND_CANCEL, amount, balanceAfter, pickupId);
    }

    @Override
    @Transactional
    public void transferOnComplete(Long payerId, Long receiverId, long amount, Long pickupId) {
        // 发布方扣减已在发布时记 SPEND_PUBLISH，完成时只对接单方入账。
        long balanceAfter = userService.applyPointChange(receiverId, amount, null);
        writeTransaction(receiverId, PointTransactionType.INCOME_COMPLETE, amount, balanceAfter, pickupId);
    }

    @Override
    public PageResult<PointTransactionItem> queryTransactions(Long userId, PointTransactionType type,
                                                              PageQuery pageQuery) {
        LambdaQueryWrapper<PointTransaction> wrapper = Wrappers.<PointTransaction>lambdaQuery()
                .eq(PointTransaction::getUserId, userId)
                .eq(type != null, PointTransaction::getType, type)
                .orderByDesc(PointTransaction::getCreatedAt);
        Page<PointTransaction> page = pointTransactionMapper.selectPage(pageQuery.toMpPage(), wrapper);
        List<PointTransactionItem> list = page.getRecords().stream()
                .map(PointTransactionItem::from)
                .toList();
        return PageResult.of(page, list);
    }

    private void writeTransaction(Long userId, PointTransactionType type, long amount,
                                  long balanceAfter, Long pickupId) {
        PointTransaction tx = new PointTransaction();
        tx.setUserId(userId);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setRelatedPickupId(pickupId);
        // createdAt/updatedAt 由 TimeFieldFillHandler 自动填充。
        pointTransactionMapper.insert(tx);
    }
}
