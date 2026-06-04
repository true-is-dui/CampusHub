package com.campushub.dto;

import lombok.Data;

@Data
public class RatingSummaryDTO {

    private Long userId;
    private Object publisherRoleSummary;
    private Object acceptorRoleSummary;
}
