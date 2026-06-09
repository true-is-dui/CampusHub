package com.campushub.entity.enums;

/**
 * 积分流水类型，对应 point_transactions.type（见 P3 数据库设计 §7.5）。
 *
 * <p>正负号约定：入账（赠送/签到/取消退回/完成入账）amount 为正，出账（发布扣减）amount 为负。
 * 流水只记录稳定的成功变动；积分不足导致的发布失败、当日重复签到等不写流水。
 */
public enum PointTransactionType {
    /** 实名认证赠送：认证通过赠送 100 积分，每个学号仅一次（+100） */
    EARN_VERIFICATION,
    /** 每日签到：每日首次签到赠送 5 积分（+5） */
    EARN_CHECK_IN,
    /** 发布扣减：发布有报酬代取时从发布方账户扣减报酬积分（负数） */
    SPEND_PUBLISH,
    /** 取消退回：待接单阶段取消有报酬代取，退回发布方账户（正数） */
    REFUND_CANCEL,
    /** 完成入账：发布方确认完成后，报酬积分转入接单方账户（正数） */
    INCOME_COMPLETE
}
