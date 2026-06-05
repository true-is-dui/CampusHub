package com.campushub.entity.enums;

/**
 * 站内通知类型，对应 notifications.type（见 P3 数据库设计 §7.9）。
 */
public enum NotificationType {
    /** 系统通知：通用系统提示 */
    SYSTEM,
    /** 实名认证通知：认证通过/驳回 */
    VERIFICATION,
    /** 代取业务通知：被接单、完成凭证上传、确认完成 */
    PICKUP,
    /** 支付通知：支付成功、关闭、退款、结算 */
    PAYMENT,
    /** 评价通知：收到评价 */
    EVALUATION
}
