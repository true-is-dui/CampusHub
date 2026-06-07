package com.campushub.dto.evaluation;

import com.campushub.dto.pickup.UserSummary;
import lombok.Builder;
import lombok.Getter;

/**
 * 评价资格，对应 {@code api_design.yaml} 的 {@code EvaluationEligibility}。
 *
 * <p>供前端决定是否展示评价入口；仅作展示辅助，提交评价时后端仍重新校验。
 * 可评价时 {@code canEvaluate=true}、{@code reviewee} 为被评价人公开摘要、{@code reason} 为空；
 * 不可评价时 {@code canEvaluate=false}、{@code reason} 说明原因（{@code reviewee} 可为空）。
 */
@Getter
@Builder
public class EvaluationEligibility {

    private final boolean canEvaluate;
    /** 被评价人公开摘要；不可评价时可为 null。 */
    private final UserSummary reviewee;
    /** 不可评价原因；可评价时为 null。 */
    private final EvaluationEligibilityReason reason;
}
