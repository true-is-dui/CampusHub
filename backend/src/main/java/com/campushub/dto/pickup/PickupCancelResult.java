package com.campushub.dto.pickup;

import com.campushub.entity.enums.PickupCancelReason;
import com.campushub.entity.enums.PickupStatus;
import lombok.Builder;
import lombok.Getter;

/**
 * 取消代取服务响应，对应 {@code api_design.yaml}
 * {@code /pickup-requests/{id}/cancel} 的 data（{@code status} + {@code cancelReason}）。
 *
 * <p>status 固定流转为 CANCELLED；cancelReason=USER_CANCELLED（发布方主动取消）。
 * 有报酬服务取消时将发布时扣减的积分退回发布方账户，积分变动经 PointService。
 */
@Getter
@Builder
public class PickupCancelResult {

    private final PickupStatus status;
    private final PickupCancelReason cancelReason;
}
