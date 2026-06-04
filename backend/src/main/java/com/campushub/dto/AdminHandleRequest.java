package com.campushub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminHandleRequest {

    @NotBlank
    private String result;

    private String reason;
}
