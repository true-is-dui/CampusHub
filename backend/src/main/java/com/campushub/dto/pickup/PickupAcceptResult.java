package com.campushub.dto.pickup;

import com.campushub.entity.enums.PickupStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 接单响应，对应 {@code api_design.yaml} 的 {@code PickupAcceptResult}。
 * 接单成功后 status=IN_PROGRESS，acceptedAt 为接单时间。
 */
@Getter
@Builder
public class PickupAcceptResult {

    private final PickupStatus status;
    private final LocalDateTime acceptedAt;
}
