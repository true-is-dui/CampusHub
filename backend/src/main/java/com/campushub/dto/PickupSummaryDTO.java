package com.campushub.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PickupSummaryDTO {

    private Long pickupId;
    private String campus;
    private String pickupLocation;
    private String deliveryLocation;
    private String itemDescriptionPreview;
    private String rewardType;
    private BigDecimal rewardAmount;
    private String status;
    private String cancelReason;
    private Object publisher;
    private String acceptDeadline;
    private String createdAt;
    private String completedAt;
}
