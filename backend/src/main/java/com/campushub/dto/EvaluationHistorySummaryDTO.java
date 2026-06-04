package com.campushub.dto;

import lombok.Data;

@Data
public class EvaluationHistorySummaryDTO {

    private Long evaluationId;
    private String revieweeRoleInBusiness;
    private String ratingLevel;
    private String contentPreview;
    private String createdAt;
}
