package com.campushub.dto.pickup;

import com.campushub.entity.enums.PickupStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 发布代取服务的响应，对应 {@code api_design.yaml} 的 {@code PickupCreateResult}。
 *
 * <p>无报酬服务：status=WAITING_ACCEPT，payEntry/expireAt 为空。
 * 有报酬服务：status=WAITING_PAYMENT，payEntry 为支付入口、expireAt 为待支付截止时间。
 */
@Getter
@Builder
public class PickupCreateResult {

    private final Long pickupId;
    private final PickupStatus status;
    /** 支付宝沙箱支付页面 URL；无报酬服务为空。 */
    private final String payEntry;
    /** 待支付截止时间，MVP 固定为预付款创建后 3 分钟；无报酬服务为空。 */
    private final LocalDateTime expireAt;
}
