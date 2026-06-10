package com.campushub.dto.evaluation;

import com.campushub.dto.pickup.UserSummary;
import com.campushub.entity.Evaluation;
import com.campushub.entity.enums.PickupParticipantRole;
import com.campushub.entity.enums.RatingLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 某个代取服务下的一条评价展示项。
 *
 * <p>用于代取详情页展示双方互评；评价表中只存被评价人在业务中的角色，评价者角色可由双方关系推导。
 */
@Getter
@Builder
public class PickupEvaluationItem {

    private final Long evaluationId;
    private final UserSummary reviewer;
    private final PickupParticipantRole reviewerRoleInBusiness;
    private final UserSummary reviewee;
    private final PickupParticipantRole revieweeRoleInBusiness;
    private final RatingLevel ratingLevel;
    private final String content;
    private final LocalDateTime createdAt;

    public static PickupEvaluationItem from(Evaluation evaluation, UserSummary reviewer, UserSummary reviewee,
                                            PickupParticipantRole reviewerRole) {
        return PickupEvaluationItem.builder()
                .evaluationId(evaluation.getId())
                .reviewer(reviewer)
                .reviewerRoleInBusiness(reviewerRole)
                .reviewee(reviewee)
                .revieweeRoleInBusiness(evaluation.getRevieweeRole())
                .ratingLevel(evaluation.getRatingLevel())
                .content(evaluation.getContent())
                .createdAt(evaluation.getCreatedAt())
                .build();
    }
}
