package com.campushub.service;

import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.notification.NotificationItem;
import com.campushub.entity.enums.NotificationType;

/**
 * 站内通知服务（Should 级能力）。
 *
 * <p>MVP 采用数据库通知记录 + 前端主动刷新 / 低频轮询未读数量，不引入 WebSocket 或消息队列
 * （见 P2 ADR-004）。{@link #createNotice} 由各业务 owner service 在关键状态变更后调用，
 * 查询类方法供 {@code /users/me/notifications*} 接口使用。
 */
public interface NotificationService {

    /**
     * 创建一条站内通知（业务事件触发）。被调用方应在自身事务内调用；
     * 实现仅做单条 insert，不向上抛业务异常，避免阻断主业务流。
     *
     * @param receiverId   接收人用户 ID
     * @param type         通知类型
     * @param title        标题
     * @param content      内容
     * @param businessType 关联业务类型（如 PICKUP_REQUEST），可为 null
     * @param businessId   关联业务主键（如 pickupId），可为 null
     */
    void createNotice(Long receiverId, NotificationType type, String title, String content,
                      String businessType, Long businessId);

    /** 分页查询当前用户的通知，按创建时间倒序。 */
    PageResult<NotificationItem> queryMyNotices(Long userId, PageQuery pageQuery);

    /** 查询当前用户的未读通知数量。 */
    long countUnread(Long userId);

    /**
     * 将指定通知标记为已读。仅接收者本人可操作。
     *
     * @throws com.campushub.common.BusinessException 通知不存在（404）或非接收者（403）
     */
    void markRead(Long notificationId, Long userId);
}
