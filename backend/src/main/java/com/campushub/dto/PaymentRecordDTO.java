package com.campushub.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRecordDTO {

    private Long paymentId;
    private Long payerId;
    private Long receiverId;
    private String businessType;
    private BigDecimal amount;
    private String status;
    private String expireAt;
    private String closeReason;
    private String statusChangedAt;
    private String createdAt;
}
