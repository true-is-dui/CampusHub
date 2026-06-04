package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campushub.entity.enums.BusinessType;
import com.campushub.entity.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_records")
public class PaymentRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long payerId;
    private Long receiverId;
    private BigDecimal amount;
    private BusinessType businessType;
    private String businessTraceNo;
    private String outTradeNo;
    private String tradeNo;
    private PaymentStatus status;
    private LocalDateTime expireAt;
    private String closeReason;
    private LocalDateTime statusChangedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
