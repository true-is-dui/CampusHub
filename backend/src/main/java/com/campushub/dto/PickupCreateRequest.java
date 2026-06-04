package com.campushub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PickupCreateRequest {

    @NotBlank
    private String campus;

    @NotBlank
    private String pickupLocation;

    @NotBlank
    private String deliveryLocation;

    @NotBlank
    private String itemDescription;

    @NotBlank
    private String rewardType;

    private BigDecimal rewardAmount;

    @NotBlank
    private String acceptDeadline;
}
