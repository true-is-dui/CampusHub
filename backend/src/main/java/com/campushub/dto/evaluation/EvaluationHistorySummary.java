package com.campushub.dto.evaluation;

import com.campushub.entity.Evaluation;
import com.campushub.entity.enums.PickupParticipantRole;
import com.campushub.entity.enums.RatingLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 用户收到的历史评价列表项，对应 {@code api_design.yaml} 的 {@code EvaluationHistorySummary}。
 *
 * <p>公开信誉展示项：只暴露被评价角色、评分等级、内容预览与时间，不暴露评价者身份与
 * 完整内容（评价详情接口有意不做，见 backend/CLAUDE.md 第四批决策）。
 */
@Getter
@Builder
public class EvaluationHistorySummary {

    /** 内容预览长度上限，超出截断。 */
    private static final int PREVIEW_LEN = 50;

    private final Long evaluationId;
    private final PickupParticipantRole revieweeRoleInBusiness;
    private final RatingLevel ratingLevel;
    /** 评价内容预览；无内容时为 null。 */
    private final String contentPreview;
    private final LocalDateTime createdAt;

    /** 实体 → VO，集中 reviewee_role 映射与内容预览截断。 */
    public static EvaluationHistorySummary from(Evaluation e) {
        return EvaluationHistorySummary.builder()
                .evaluationId(e.getId())
                .revieweeRoleInBusiness(e.getRevieweeRole())
                .ratingLevel(e.getRatingLevel())
                .contentPreview(preview(e.getContent()))
                .createdAt(e.getCreatedAt())
                .build();
    }

    private static String preview(String content) {
        if (content == null) {
            return null;
        }
        return content.length() <= PREVIEW_LEN ? content : content.substring(0, PREVIEW_LEN);
    }
}
