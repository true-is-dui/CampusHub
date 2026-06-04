package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campushub.entity.enums.ReviewStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("verification_reviews")
public class VerificationReview {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private ReviewStatus status;
    private Long reviewerId;
    private String rejectReason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
