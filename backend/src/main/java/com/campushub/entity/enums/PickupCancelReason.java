package com.campushub.entity.enums;

/**
 * 代取请求取消原因，对应 pickup_requests.cancel_reason。
 */
public enum PickupCancelReason {
    /** 发布方主动取消：仅允许待支付或待接单状态 */
    USER_CANCELLED,
    /** 支付超时取消：有报酬服务 3 分钟内未完成预付款 */
    PAYMENT_EXPIRED,
    /** 接单截止超时取消：待接单服务超过接单截止时间无人接单 */
    ACCEPT_DEADLINE_EXPIRED,
    /** 系统取消：后端异常处理或人工修正使用，不作为常规业务入口 */
    SYSTEM_CANCELLED
}
