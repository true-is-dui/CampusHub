package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.ErrorReason;
import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.notification.NotificationItem;
import com.campushub.entity.NotificationRecord;
import com.campushub.entity.enums.NotificationType;
import com.campushub.mapper.NotificationRecordMapper;
import com.campushub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link NotificationService} 实现。
 *
 * <p>纯数据库通知记录（无 WebSocket/MQ）。已读状态用实体的 {@code Boolean read}（is_read 列）
 * 存储，对外经 {@link NotificationItem} 映射为契约的 {@code readStatus}（UNREAD/READ）。
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRecordMapper notificationRecordMapper;

    @Override
    public void createNotice(Long receiverId, NotificationType type, String title, String content,
                             String businessType, Long businessId) {
        NotificationRecord record = new NotificationRecord();
        record.setReceiverId(receiverId);
        record.setType(type);
        record.setTitle(title);
        record.setContent(content);
        record.setBusinessType(businessType);
        record.setBusinessId(businessId);
        record.setIsRead(false);
        // createdAt/updatedAt 由 TimeFieldFillHandler 自动填充。
        notificationRecordMapper.insert(record);
    }

    @Override
    public PageResult<NotificationItem> queryMyNotices(Long userId, PageQuery pageQuery) {
        Page<NotificationRecord> page = notificationRecordMapper.selectPage(
                pageQuery.toMpPage(),
                Wrappers.<NotificationRecord>lambdaQuery()
                        .eq(NotificationRecord::getReceiverId, userId)
                        .orderByDesc(NotificationRecord::getCreatedAt));
        List<NotificationItem> list = page.getRecords().stream()
                .map(NotificationItem::from)
                .toList();
        return PageResult.of(page, list);
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRecordMapper.selectCount(
                Wrappers.<NotificationRecord>lambdaQuery()
                        .eq(NotificationRecord::getReceiverId, userId)
                        .eq(NotificationRecord::getIsRead, false));
    }

    @Override
    public void markRead(Long notificationId, Long userId) {
        NotificationRecord record = notificationRecordMapper.selectById(notificationId);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ErrorReason.RESOURCE_NOT_FOUND, "通知不存在");
        }
        if (!record.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, ErrorReason.NOT_NOTIFICATION_RECEIVER);
        }
        // 已读则幂等返回，不再写库。
        if (Boolean.TRUE.equals(record.getIsRead())) {
            return;
        }
        record.markRead();
        notificationRecordMapper.updateById(record);
    }
}
