package com.campushub.dto;

import lombok.Data;

@Data
public class PickupCreateResultDTO {

    private Long pickupId;
    private String status;
    private String payEntry;
    private String expireAt;
}
