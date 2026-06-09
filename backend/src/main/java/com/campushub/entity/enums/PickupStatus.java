package com.campushub.entity.enums;

/**
 * 代取请求状态，对应 pickup_requests.status。
 * 状态流转见 P3 数据库设计 §7.3。
 */
public enum PickupStatus {
    /** 待接单：可在大厅展示，等待其他认证用户接单（有报酬服务发布即扣减积分后进入） */
    WAITING_ACCEPT,
    /** 进行中：已被接单，服务进行中 */
    IN_PROGRESS,
    /** 已完成：发布方确认完成（终态） */
    COMPLETED,
    /** 已取消：发布方取消、接单截止超时或系统取消（终态） */
    CANCELLED
}
