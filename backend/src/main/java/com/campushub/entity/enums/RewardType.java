package com.campushub.entity.enums;

/**
 * 代取报酬类型，对应 pickup_requests.reward_type。
 */
public enum RewardType {
    /** 有报酬：必须填写 1-200 元报酬金额，需经支付宝沙箱预付款 */
    PAID,
    /** 无报酬：不涉及资金流转，发布后直接进入待接单 */
    UNPAID
}
