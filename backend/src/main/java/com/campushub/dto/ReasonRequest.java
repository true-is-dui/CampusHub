package com.campushub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReasonRequest {

    @NotBlank
    private String reason;
}
