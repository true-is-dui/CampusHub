package com.campushub.service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 创建预付款入口的结果，由 {@code PaymentService.createPrepay} 返回给代取服务模块。
 *
 * <p>代取服务据此把代取请求关联到支付记录（{@code PickupRequest.paymentId}）并向前端
 * 返回支付入口与过期时间。放在 {@code service.dto}：跨模块（支付→代取）传递的业务层载体。
 */
@Getter
@Builder
public class PrepayResult {

    private final Long paymentId;
    /** 支付宝沙箱支付页面 URL（本批为占位值，第六批接入真实网关后替换）。 */
    private final String payEntry;
    private final LocalDateTime expireAt;
}
