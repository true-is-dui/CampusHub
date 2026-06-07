package com.campushub.dto.pickup;

import com.campushub.entity.enums.PaymentStatus;
import com.campushub.entity.enums.PickupStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 完成确认响应，对应 {@code api_design.yaml}
 * {@code /pickup-requests/{id}/completion-confirmation} 的 data。
 *
 * <p>status 固定流转为 COMPLETED；有报酬服务 paymentStatus 为 SETTLED（本批结算留桩，
 * 仅标记支付记录状态，无真实资金），无报酬服务 paymentStatus 为空。
 */
@Getter
@Builder
public class CompletionConfirmResult {

    private final PickupStatus status;
    /** 有报酬服务结算后的支付状态（SETTLED）；无报酬服务为空。 */
    private final PaymentStatus paymentStatus;
    private final LocalDateTime completedAt;
}
