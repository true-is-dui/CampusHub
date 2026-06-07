package com.campushub.service;

import com.campushub.entity.enums.BusinessType;
import com.campushub.entity.enums.PaymentStatus;
import com.campushub.service.dto.PrepayResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付服务，维护平台内部支付记录与支付状态，对应 {@code class_design.md} 的 {@code PaymentService}。
 *
 * <p>只理解支付语义（创建预付款、关闭、退款、结算、查状态），<b>不理解具体代取业务对象</b>；
 * 代取请求到支付记录的关联由 {@code PickupRequest.paymentId} 维护。外部支付网关细节由
 * {@code PaymentGateway} 适配（第六批接入支付宝沙箱）。
 *
 * <p><b>本批（第五批）为占位实现</b>：{@code createPrepay} 落一条 WAITING_PAY 记录并返回占位
 * payEntry；{@code cancelWaitingPayment} 标记关闭。退款 / 结算仅做状态标记（无真实资金）。
 * 接口签名按 class_design 定稿，第六批填真实支付宝时 {@code PickupService} 零返工。
 */
public interface PaymentService {

    /**
     * 创建支付宝沙箱预付款入口，落一条 WAITING_PAY 支付记录。
     *
     * @param payerId         付款方（代取发布方）
     * @param amount          支付金额
     * @param expireAt        待支付截止时间（由调用方按 MVP 规则算定，当前为创建后 3 分钟）
     * @param businessType    业务类型（追踪用，当前仅 PICKUP_REQUEST）
     * @param businessTraceNo 业务追踪号（幂等/回调定位用，必须唯一）
     * @return paymentId、支付入口与过期时间
     */
    PrepayResult createPrepay(Long payerId, BigDecimal amount, LocalDateTime expireAt,
                              BusinessType businessType, String businessTraceNo);

    /** 关闭尚未支付的支付记录（发布方取消待支付服务、或支付超时）。 */
    void cancelWaitingPayment(Long paymentId, String closeReason);

    /** 已预付款服务取消时退款（本批占位：仅标记 REFUNDED，无真实资金流转）。 */
    void refundPayment(Long paymentId);

    /** 发布方确认完成后向接单方结算（本批占位：仅标记 SETTLED，无真实资金流转）。 */
    void settlePayment(Long paymentId, Long receiverId);

    /** 查询支付记录当前状态。 */
    PaymentStatus queryPaymentStatus(Long paymentId);
}
