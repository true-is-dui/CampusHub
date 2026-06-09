package com.campushub.dto.pickup;

import com.campushub.entity.enums.PickupStatus;
import lombok.Builder;
import lombok.Getter;

/**
 * 发布代取服务的响应，对应 {@code api_design.yaml} 的 {@code PickupCreateResult}。
 *
 * <p>无报酬与有报酬服务发布成功后均直接进入 WAITING_ACCEPT：有报酬服务在发布时已从发布方
 * 账户扣减积分（余额不足则发布失败 409 INSUFFICIENT_POINTS），不再有独立的待支付态。
 */
@Getter
@Builder
public class PickupCreateResult {

    private final Long pickupId;
    private final PickupStatus status;
}
