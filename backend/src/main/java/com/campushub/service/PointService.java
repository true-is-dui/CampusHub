package com.campushub.service;

import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.point.CheckInResult;
import com.campushub.dto.point.PointTransactionItem;
import com.campushub.entity.enums.PointTransactionType;

/**
 * 积分服务，维护用户积分余额与积分流水，是平台内虚拟积分的唯一入口，对应
 * {@code class_design.md} 的 {@code PointService}。积分不可充值提现，不依赖任何第三方支付网关。
 *
 * <p>所有积分变动（认证赠送、签到、代取发布/取消/完成）都同步更新 {@code users.point_balance}
 * 并写入 {@code point_transactions} 流水（含 {@code balanceAfter}），由内部模板
 * {@code applyChange} 固定「读用户 → 校验 → 改余额 → 写流水」骨架，保证余额与流水一致。
 *
 * <p>余额扣减/退回/转入须与对应业务状态变更在<b>同一事务</b>内完成：代取相关方法由
 * {@code PickupService} 的 {@code @Transactional} 方法调用，事务边界在调用方；签到/赠送方法
 * 自身带事务。{@code PointService} 不理解代取业务状态，{@code relatedPickupId} 仅作流水溯源。
 */
public interface PointService {

    /**
     * 实名认证通过后赠送初始积分（当前 MVP 100）。由认证审核模块在审核通过时调用，
     * 处于该流程的事务内；写 {@code EARN_VERIFICATION} 流水。每学号仅一次的去重由调用方
     * （认证通过本身每学号一次）保证，本方法只负责加分与留痕。
     */
    void grantInitialPoints(Long userId);

    /**
     * 每日首次签到赠送积分（当前 MVP 5），按 {@code users.last_check_in_date} 去重；
     * 当日已签到抛业务冲突（409 {@code ALREADY_CHECKED_IN_TODAY}）。写 {@code EARN_CHECK_IN} 流水。
     *
     * @return 本次获得积分与签到后余额
     */
    CheckInResult checkIn(Long userId);

    /** 查询用户当前积分余额。 */
    long getBalance(Long userId);

    /**
     * 发布有报酬代取时扣减发布方积分；余额不足抛 409 {@code INSUFFICIENT_POINTS}（不写流水）。
     * 写 {@code SPEND_PUBLISH} 流水（amount 为负）。须在发布事务内调用。
     */
    void spendForPublish(Long userId, long amount, Long pickupId);

    /**
     * 取消有报酬代取（待接单阶段）时退回发布方积分，写 {@code REFUND_CANCEL} 流水（正数）。
     * 须在取消事务内调用。
     */
    void refundForCancel(Long userId, long amount, Long pickupId);

    /**
     * 确认完成时把报酬积分转入接单方账户，写接单方 {@code INCOME_COMPLETE} 流水（正数）。
     *
     * <p>发布方的扣减已在发布时记 {@code SPEND_PUBLISH}，故完成时只对接单方入账，不再二次扣减
     * 发布方（与 class_design.md「SPEND 已在发布时记，完成记 INCOME_COMPLETE」一致）。
     * {@code payerId} 仅用于流水溯源语义对齐，当前实现只对 {@code receiverId} 入账。须在完成事务内调用。
     */
    void transferOnComplete(Long payerId, Long receiverId, long amount, Long pickupId);

    /**
     * 分页查询用户积分流水，可按流水类型筛选（{@code type} 为 null 查全部），按创建时间倒序。
     */
    PageResult<PointTransactionItem> queryTransactions(Long userId, PointTransactionType type, PageQuery pageQuery);
}
