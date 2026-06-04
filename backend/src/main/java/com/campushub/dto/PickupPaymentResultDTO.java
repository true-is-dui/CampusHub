package com.campushub.dto;

import lombok.Data;

@Data
public class PickupPaymentResultDTO {

    private String status;
    private String payEntry;
    private String expireAt;
}
