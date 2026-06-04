package com.campushub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerificationSubmitRequest {

    @NotBlank
    private String studentId;

    @NotBlank
    private String realName;
}
