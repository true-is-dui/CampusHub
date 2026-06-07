package com.campushub.dto.notification;

/**
 * 通知读状态，对应 {@code api_design.yaml} 中 {@code NotificationItem.readStatus} 的枚举值。
 *
 * <p>仅用于 API 响应 VO：数据库与实体（{@link com.campushub.entity.NotificationRecord#getIsRead()}）
 * 用 {@code Boolean read}（is_read 列）存储已读状态，没有对应的领域枚举列，故本枚举归在
 * {@code dto.notification} 下而非 {@code entity.enums}（那里的枚举都有真实的数据库列映射）。
 * 由 {@link NotificationItem#from} 做 {@code read==true ? READ : UNREAD} 的映射。
 */
public enum ReadStatus {
    /** 未读 */
    UNREAD,
    /** 已读 */
    READ
}
