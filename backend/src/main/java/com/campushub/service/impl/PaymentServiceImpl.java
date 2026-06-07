package com.campushub.service.impl;

import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.ErrorReason;
import com.campushub.entity.PaymentRecord;
import com.campushub.entity.enums.BusinessType;
import com.campushub.entity.enums.PaymentStatus;
import com.campushub.mapper.PaymentRecordMapper;
import com.campushub.service.PaymentService;
import com.campushub.service.dto.PrepayResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@link PaymentService} 的第五批占位实现。
 *
 * <p>只维护 payment_records 的状态机，不接入真实支付宝沙箱。{@code createPrepay} 写入一条
 * WAITING_PAY 记录并返回占位 payEntry；其余方法做状态标记。第六批接入
 * {@code PaymentGateway} / {@code AlipaySandboxPaymentGateway} 后，把占位 payEntry 与
 * 退款 / 结算的真实网关调用补上，接口与 {@code PickupService} 的依赖不变。
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    /** 占位支付入口前缀，第六批替换为支付宝沙箱返回的真实跳转 URL。 */
    private static final String PLACEHOLDER_PAY_ENTRY_PREFIX = "https://sandbox.placeholder/pay/";

    private final PaymentRecordMapper paymentRecordMapper;

    @Override
    @Transactional
    public PrepayResult createPrepay(Long payerId, BigDecimal amount, LocalDateTime expireAt,
                                     BusinessType businessType, String businessTraceNo) {
        String outTradeNo = "OTN" + UUID.randomUUID().toString().replace("-", "");

        PaymentRecord record = new PaymentRecord();
        record.setPayerId(payerId);
        record.setBusinessType(businessType);
        record.setBusinessTraceNo(businessTraceNo);
        record.setAmount(amount);
        record.setOutTradeNo(outTradeNo);
        record.setStatus(PaymentStatus.WAITING_PAY);
        record.setExpireAt(expireAt);
        record.setStatusChangedAt(LocalDateTime.now());
        // 占位 payEntry：第六批由支付宝沙箱创建后回填。
        record.setPayEntry(PLACEHOLDER_PAY_ENTRY_PREFIX + outTradeNo);
        paymentRecordMapper.insert(record);

        return PrepayResult.builder()
                .paymentId(record.getId())
                .payEntry(record.getPayEntry())
                .expireAt(record.getExpireAt())
                .build();
    }

    @Override
    @Transactional
    public void cancelWaitingPayment(Long paymentId, String closeReason) {
        PaymentRecord record = requirePayment(paymentId);
        if (!record.isWaitingPay()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    ErrorReason.DUPLICATE_OR_CONFLICTED_OPERATION, "支付记录当前状态不允许关闭");
        }
        record.markClosed(closeReason);
        paymentRecordMapper.updateById(record);
    }

    @Override
    @Transactional
    public void refundPayment(Long paymentId) {
        // 占位：仅标记退款状态，无真实资金流转（第六批接入支付宝沙箱退款）。
        PaymentRecord record = requirePayment(paymentId);
        record.markRefunded();
        paymentRecordMapper.updateById(record);
    }

    @Override
    @Transactional
    public void settlePayment(Long paymentId, Long receiverId) {
        // 占位：仅标记结算状态，无真实转账（第六批接入支付宝沙箱结算）。
        PaymentRecord record = requirePayment(paymentId);
        record.markSettled(receiverId);
        paymentRecordMapper.updateById(record);
    }

    @Override
    public PaymentStatus queryPaymentStatus(Long paymentId) {
        return requirePayment(paymentId).getStatus();
    }

    private PaymentRecord requirePayment(Long paymentId) {
        PaymentRecord record = paymentRecordMapper.selectById(paymentId);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ErrorReason.RESOURCE_NOT_FOUND,
                    "支付记录不存在");
        }
        return record;
    }
}
