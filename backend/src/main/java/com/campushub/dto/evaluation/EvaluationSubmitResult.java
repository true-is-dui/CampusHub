package com.campushub.dto.evaluation;

import lombok.Builder;
import lombok.Getter;

/**
 * 提交评价的返回体，对应 {@code api_design.yaml} 中 {@code POST /pickup-requests/{pickupId}/evaluations}
 * 成功响应的内联 {@code data}（仅含 {@code evaluationId}）。
 */
@Getter
@Builder
public class EvaluationSubmitResult {

    private final Long evaluationId;
}
