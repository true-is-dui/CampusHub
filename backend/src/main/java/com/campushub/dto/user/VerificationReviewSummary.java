package com.campushub.dto.user;

import com.campushub.entity.enums.ReviewStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 管理员审核列表项，对应 api_design.yaml 的 VerificationReviewSummary。 */
@Getter
@Builder
public class VerificationReviewSummary {

    private final Long id;
    private final Long userId;
    private final String username;
    private final String nickname;
    private final String studentId;
    private final String realName;
    private final ReviewStatus status;
    private final String rejectReason;
    private final Long reviewerId;
    private final LocalDateTime createdAt;
    private final LocalDateTime reviewedAt;
}
