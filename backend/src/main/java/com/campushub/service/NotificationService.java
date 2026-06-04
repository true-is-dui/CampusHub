package com.campushub.service;

import com.campushub.entity.enums.BusinessType;
import com.campushub.entity.enums.NotificationType;

import java.util.Map;

public interface NotificationService {
    void createNotice(Long receiverId, NotificationType type, String title, String content, BusinessType businessType, Long businessId);
    Map<String, Object> queryMyNotices(Long userId, int page, int pageSize);
    long countUnread(Long userId);
    void markRead(Long noticeId, Long userId);
}
