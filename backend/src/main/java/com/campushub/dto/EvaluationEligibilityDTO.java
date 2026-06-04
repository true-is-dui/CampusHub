package com.campushub.dto;

import lombok.Data;

@Data
public class EvaluationEligibilityDTO {

    private Boolean canEvaluate;
    private Object reviewee;
    private String reason;
}
