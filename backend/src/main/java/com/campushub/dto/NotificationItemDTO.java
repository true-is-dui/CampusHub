package com.campushub.dto;

import lombok.Data;

@Data
public class NotificationItemDTO {

    private Long notificationId;
    private String type;
    private String title;
    private String content;
    private String businessType;
    private Long businessId;
    private String readStatus;
    private String createdAt;
}
