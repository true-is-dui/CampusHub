package com.campushub.dto.evaluation;

import com.campushub.dto.pickup.UserSummary;
import com.campushub.entity.Evaluation;
import com.campushub.entity.enums.PickupParticipantRole;
import com.campushub.entity.enums.RatingLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 当前用户收到的某次代取服务评价详情。
 *
 * <p>用于站内评价通知落地页：只允许被评价人本人按 pickupId 查询，因此可以展示评价者摘要和完整内容。
 */
@Getter
@Builder
public class ReceivedEvaluationDetail {

    private final Long evaluationId;
    private final UserSummary reviewer;
    private final PickupParticipantRole revieweeRoleInBusiness;
    private final RatingLevel ratingLevel;
    private final String content;
    private final LocalDateTime createdAt;

    public static ReceivedEvaluationDetail from(Evaluation evaluation, UserSummary reviewer) {
        return ReceivedEvaluationDetail.builder()
                .evaluationId(evaluation.getId())
                .reviewer(reviewer)
                .revieweeRoleInBusiness(evaluation.getRevieweeRole())
                .ratingLevel(evaluation.getRatingLevel())
                .content(evaluation.getContent())
                .createdAt(evaluation.getCreatedAt())
                .build();
    }
}
