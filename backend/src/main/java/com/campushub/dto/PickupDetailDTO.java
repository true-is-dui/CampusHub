package com.campushub.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PickupDetailDTO extends PickupSummaryDTO {

    private String itemDescription;
    private Object acceptor;
    private String acceptedAt;
}
