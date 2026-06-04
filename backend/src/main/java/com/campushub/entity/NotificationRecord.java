package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campushub.entity.enums.BusinessType;
import com.campushub.entity.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notifications")
public class NotificationRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long receiverId;
    private NotificationType type;
    private String title;
    private String content;
    private BusinessType businessType;
    private Long businessId;
    private String readStatus;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
