package com.campushub.dto.pickup;

import com.campushub.entity.enums.PaymentStatus;
import com.campushub.entity.enums.PickupCancelReason;
import com.campushub.entity.enums.PickupStatus;
import lombok.Builder;
import lombok.Getter;

/**
 * 取消代取服务响应，对应 {@code api_design.yaml}
 * {@code /pickup-requests/{id}/cancel} 的 data。
 *
 * <p>status 固定流转为 CANCELLED；cancelReason=USER_CANCELLED（发布方主动取消）。
 * paymentStatus：WAITING_PAYMENT 取消时为 CLOSED；WAITING_ACCEPT 取消时无报酬为空、
 * 有报酬已预付款为 REFUNDED（本批退款留桩，仅标记状态）。
 */
@Getter
@Builder
public class PickupCancelResult {

    private final PickupStatus status;
    /** 关联支付记录的状态（CLOSED 或 REFUNDED）；无报酬服务为空。 */
    private final PaymentStatus paymentStatus;
    private final PickupCancelReason cancelReason;
}
