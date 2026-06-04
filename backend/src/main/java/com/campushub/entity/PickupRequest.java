package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campushub.entity.enums.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pickup_requests")
public class PickupRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long publisherId;
    private Long acceptorId;
    private String campus;
    private String pickupLocation;
    private String deliveryLocation;
    private String itemDescription;
    private Long pickupCredentialFileId;
    private RewardType rewardType;
    private BigDecimal rewardAmount;
    private Long paymentId;
    private PickupStatus status;
    private LocalDateTime acceptDeadline;
    private LocalDateTime acceptedAt;
    private Long completionProofFileId;
    private LocalDateTime completedAt;
    private PickupCancelReason cancelReason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
