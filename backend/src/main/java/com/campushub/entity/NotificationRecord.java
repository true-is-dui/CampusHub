package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campushub.entity.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知实体，映射 notifications 表（Should 级能力）。
 *
 * <p>MVP 采用数据库通知记录 + 前端主动刷新 / 低频轮询未读数量，
 * 不引入 WebSocket 或消息队列。businessType + businessId 用于回溯
 * 通知关联的业务上下文。
 */
@Data
@TableName("notifications")
public class NotificationRecord {

    /** 主键，数据库自增；对应 API notificationId */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人用户 ID */
    private Long receiverId;

    /** 通知类型 */
    private NotificationType type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 关联业务类型，例如 PICKUP_REQUEST */
    private String businessType;

    /** 关联业务 ID，例如 pickup_requests.id */
    private Long businessId;

    /** 是否已读，false=未读，true=已读（映射 TINYINT(1)） */
    @TableField("is_read")
    private Boolean read;

    /** 已读时间 */
    private LocalDateTime readAt;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ---------------- 领域方法（仅供 Service 层内部调用） ----------------

    /** 标记为已读 */
    public void markRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }
}
