package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.dto.NotificationItemDTO;
import com.campushub.entity.NotificationRecord;
import com.campushub.entity.enums.BusinessType;
import com.campushub.entity.enums.NotificationType;
import com.campushub.mapper.NotificationRecordMapper;
import com.campushub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRecordMapper notificationRecordMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void createNotice(Long receiverId, NotificationType type, String title, String content,
                             BusinessType businessType, Long businessId) {
        NotificationRecord record = new NotificationRecord();
        record.setReceiverId(receiverId);
        record.setType(type);
        record.setTitle(title);
        record.setContent(content);
        record.setBusinessType(businessType);
        record.setBusinessId(businessId);
        record.setReadStatus("UNREAD");
        notificationRecordMapper.insert(record);
    }

    public Map<String, Object> queryMyNotices(Long userId, int page, int pageSize) {
        Page<NotificationRecord> pageObj = new Page<>(page, pageSize);
        QueryWrapper<NotificationRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("receiver_id", userId).orderByDesc("created_at");
        IPage<NotificationRecord> result = notificationRecordMapper.selectPage(pageObj, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("list", result.getRecords().stream().map(this::toNotificationItemDTO).toList());
        return data;
    }

    public long countUnread(Long userId) {
        QueryWrapper<NotificationRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("receiver_id", userId).eq("read_status", "UNREAD");
        return notificationRecordMapper.selectCount(wrapper);
    }

    public void markRead(Long noticeId, Long userId) {
        NotificationRecord record = notificationRecordMapper.selectById(noticeId);
        if (record == null || !record.getReceiverId().equals(userId)) {
            throw new BusinessException(40401, "通知不存在");
        }
        record.setReadStatus("READ");
        notificationRecordMapper.updateById(record);
    }

    private NotificationItemDTO toNotificationItemDTO(NotificationRecord record) {
        NotificationItemDTO dto = new NotificationItemDTO();
        dto.setNotificationId(record.getId());
        dto.setType(record.getType() != null ? record.getType().name() : null);
        dto.setTitle(record.getTitle());
        dto.setContent(record.getContent());
        dto.setBusinessType(record.getBusinessType() != null ? record.getBusinessType().name() : null);
        dto.setBusinessId(record.getBusinessId());
        dto.setReadStatus(record.getReadStatus());
        dto.setCreatedAt(record.getCreatedAt() != null ? record.getCreatedAt().format(DT_FMT) : null);
        return dto;
    }
}
