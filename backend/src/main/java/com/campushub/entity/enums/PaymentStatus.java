package com.campushub.entity.enums;

/**
 * 支付记录状态，对应 payment_records.status。
 * 仅保留五类稳定状态；支付失败、回调失败等异常不写入此枚举，
 * 通过 failure_reason 或应用日志记录（见 P3 数据库设计 §7.5）。
 */
public enum PaymentStatus {
    /** 待支付：已创建支付入口，等待支付宝沙箱付款 */
    WAITING_PAY,
    /** 已支付：预付款成功，代取服务可进入待接单 */
    PAID,
    /** 已关闭：待支付未完成付款时由发布方取消或超时关闭 */
    CLOSED,
    /** 已退款：已支付服务在待接单阶段取消或接单截止超时后退款 */
    REFUNDED,
    /** 已结算：发布方确认完成后结算给接单方 */
    SETTLED
}
