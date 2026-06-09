package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.ErrorReason;
import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.point.CheckInResult;
import com.campushub.dto.point.PointTransactionItem;
import com.campushub.entity.PointTransaction;
import com.campushub.entity.User;
import com.campushub.entity.enums.PointTransactionType;
import com.campushub.mapper.PointTransactionMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * {@link PointService} 实现。
 *
 * <p>积分变动统一收口到内部模板 {@link #applyChange}：固定「读用户 → 改余额（领域方法）→
 * 条件更新落库 → 写流水（含 balanceAfter）」骨架，保证每次变动余额与流水一致、不漏 balanceAfter。
 * 各公开方法只表达自身前置校验差异（余额是否充足、当日是否已签到）。
 *
 * <p>余额落库用<b>条件更新</b>（{@code WHERE id=? AND point_balance=<旧值>}，影响行数 0 视为并发
 * 改变）保证并发安全，与代取的状态条件更新一致，不引乐观锁 version 列。
 */
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    /** 实名认证通过赠送积分（当前 MVP）。 */
    private static final long INITIAL_POINTS = 100L;
    /** 每日签到赠送积分（当前 MVP）。 */
    private static final int CHECK_IN_POINTS = 5;

    private final UserMapper userMapper;
    private final PointTransactionMapper pointTransactionMapper;

    @Override
    @Transactional
    public void grantInitialPoints(Long userId) {
        applyChange(userId, PointTransactionType.EARN_VERIFICATION, INITIAL_POINTS, null);
    }

    @Override
    @Transactional
    public CheckInResult checkIn(Long userId) {
        User user = requireUser(userId);
        LocalDate today = LocalDate.now();
        if (today.equals(user.getLastCheckInDate())) {
            throw new BusinessException(ErrorCode.CONFLICT, ErrorReason.ALREADY_CHECKED_IN_TODAY);
        }
        // 签到去重日期与余额同笔更新；条件带旧签到日期，避免并发重复签到。
        long balanceAfter = user.addPoints(CHECK_IN_POINTS);
        user.setLastCheckInDate(today);
        boolean updated = userMapper.update(user, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, userId)
                .and(w -> w.isNull(User::getLastCheckInDate)
                        .or().ne(User::getLastCheckInDate, today))) > 0;
        if (!updated) {
            throw new BusinessException(ErrorCode.CONFLICT, ErrorReason.ALREADY_CHECKED_IN_TODAY);
        }
        writeTransaction(userId, PointTransactionType.EARN_CHECK_IN, CHECK_IN_POINTS, balanceAfter, null);
        return CheckInResult.builder()
                .earnedPoints(CHECK_IN_POINTS)
                .pointBalance(balanceAfter)
                .build();
    }

    @Override
    public long getBalance(Long userId) {
        User user = requireUser(userId);
        return user.getPointBalance() == null ? 0L : user.getPointBalance();
    }

    @Override
    @Transactional
    public void spendForPublish(Long userId, long amount, Long pickupId) {
        User user = requireUser(userId);
        long current = user.getPointBalance() == null ? 0L : user.getPointBalance();
        if (current < amount) {
            // 余额不足：不写流水，返回业务冲突。
            throw new BusinessException(ErrorCode.CONFLICT, ErrorReason.INSUFFICIENT_POINTS);
        }
        long balanceAfter = user.deductPoints(amount);
        // 条件更新带旧余额，防并发超扣（影响行数 0 视为余额已被并发改变）。
        requireBalanceUpdated(user, current);
        writeTransaction(userId, PointTransactionType.SPEND_PUBLISH, -amount, balanceAfter, pickupId);
    }

    @Override
    @Transactional
    public void refundForCancel(Long userId, long amount, Long pickupId) {
        applyChange(userId, PointTransactionType.REFUND_CANCEL, amount, pickupId);
    }

    @Override
    @Transactional
    public void transferOnComplete(Long payerId, Long receiverId, long amount, Long pickupId) {
        // 发布方扣减已在发布时记 SPEND_PUBLISH，完成时只对接单方入账。
        applyChange(receiverId, PointTransactionType.INCOME_COMPLETE, amount, pickupId);
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

    // ---------------- 内部模板 ----------------

    /**
     * 积分变动模板：读用户 → 改余额（领域方法）→ 条件更新落库 → 写流水。
     * 仅用于「纯加分」的变动（赠送、退回、完成入账）；带前置校验的扣减/签到在各自方法内处理。
     *
     * @param signedAmount 写入流水的带符号变动量（入账为正）；本模板只做加分故等同 user.addPoints 的入参
     */
    private void applyChange(Long userId, PointTransactionType type, long signedAmount, Long pickupId) {
        User user = requireUser(userId);
        long before = user.getPointBalance() == null ? 0L : user.getPointBalance();
        long balanceAfter = user.addPoints(signedAmount);
        requireBalanceUpdated(user, before);
        writeTransaction(userId, type, signedAmount, balanceAfter, pickupId);
    }

    /** 条件更新余额：仅当库中余额仍为 expectedBefore 时落库，0 行视为并发冲突。 */
    private void requireBalanceUpdated(User user, long expectedBefore) {
        int affected = userMapper.update(user, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, user.getId())
                .eq(User::getPointBalance, expectedBefore));
        if (affected == 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    ErrorReason.DUPLICATE_OR_CONFLICTED_OPERATION, "积分余额已被并发修改，请重试");
        }
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

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ErrorReason.RESOURCE_NOT_FOUND, "用户不存在");
        }
        return user;
    }
}
