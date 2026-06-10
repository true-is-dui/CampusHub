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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link NotificationService} 实现。
 *
 * <p>纯数据库通知记录（无 WebSocket/MQ）。已读状态用实体的 {@code Boolean read}（is_read 列）
 * 存储，对外经 {@link NotificationItem} 映射为契约的 {@code readStatus}（UNREAD/READ）。
 *
 * <p>{@code createNotice} 是业务模块在状态变更后的<b>旁路调用</b>：通知是辅助功能，
 * 发送失败不应拖垮主业务，故内部吞异常 + 记 warn 日志，不向调用方抛出。
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRecordMapper notificationRecordMapper;

    @Override
    public void createNotice(Long receiverId, NotificationType type, String title, String content,
                             String businessType, Long businessId) {
        try {
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
        } catch (Exception e) {
            // 通知是旁路功能，发送失败只记日志，不向主业务抛出（避免一条通知失败回滚主业务）。
            log.warn("createNotice failed: receiverId={}, type={}, businessId={}",
                    receiverId, type, businessId, e);
        }
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
