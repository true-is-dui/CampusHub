package com.campushub.dto;

import lombok.Data;

@Data
public class VerificationReviewDTO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String studentIdMasked;
    private String realName;
    private String status;
    private String rejectReason;
    private Long reviewerId;
    private String createdAt;
    private String reviewedAt;
}
