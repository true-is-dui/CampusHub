package com.campushub.dto;

import lombok.Data;

@Data
public class RatingRoleSummaryDTO {

    private String revieweeRoleInBusiness;
    private Integer positiveCount;
    private Integer neutralCount;
    private Integer negativeCount;
    private Integer totalCount;
    private Double positiveRate;
}
