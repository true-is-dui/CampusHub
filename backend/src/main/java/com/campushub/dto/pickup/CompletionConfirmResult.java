package com.campushub.dto.pickup;

import com.campushub.entity.enums.PickupStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 完成确认响应，对应 {@code api_design.yaml}
 * {@code /pickup-requests/{id}/completion-confirmation} 的 data（{@code status} + {@code completedAt}）。
 *
 * <p>status 固定流转为 COMPLETED；有报酬服务在确认完成时把发布时扣减的报酬积分转入接单方账户，
 * 积分变动经 PointService（无报酬服务不涉及积分）。
 */
@Getter
@Builder
public class CompletionConfirmResult {

    private final PickupStatus status;
    private final LocalDateTime completedAt;
}
