package com.campushub.dto.notification;

import com.campushub.entity.NotificationRecord;
import com.campushub.entity.enums.BusinessType;
import com.campushub.entity.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 站内通知列表项，对应 {@code api_design.yaml} 的 {@code NotificationItem}。
 *
 * <p>不直接暴露实体：实体用 {@code Boolean read}，本 VO 按契约输出
 * {@code readStatus}（UNREAD/READ）。{@code businessType} 按契约用 {@link BusinessType}
 * 枚举（当前 MVP 唯一值 PICKUP_REQUEST），{@code businessId} 可为 null（无业务关联时）。
 */
@Getter
@Builder
public class NotificationItem {

    private final Long notificationId;
    private final NotificationType type;
    private final String title;
    private final String content;
    /** 关联业务类型，当前 MVP 唯一值 PICKUP_REQUEST；无关联时为 null。 */
    private final BusinessType businessType;
    /** 关联业务主键（如 pickupId），无业务关联时为 null。 */
    private final Long businessId;
    private final ReadStatus readStatus;
    private final LocalDateTime createdAt;

    /** 实体 → VO，集中 read(Boolean)→readStatus、businessType(String)→枚举 的契约映射。 */
    public static NotificationItem from(NotificationRecord record) {
        return NotificationItem.builder()
                .notificationId(record.getId())
                .type(record.getType())
                .title(record.getTitle())
                .content(record.getContent())
                .businessType(record.getBusinessType() == null
                        ? null
                        : BusinessType.valueOf(record.getBusinessType()))
                .businessId(record.getBusinessId())
                .readStatus(Boolean.TRUE.equals(record.getIsRead()) ? ReadStatus.READ : ReadStatus.UNREAD)
                .createdAt(record.getCreatedAt())
                .build();
    }
}
