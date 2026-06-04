package com.campushub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EvaluationCreateRequest {

    @NotBlank
    private String ratingLevel;

    private String content;
}
