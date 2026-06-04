package com.campushub.service;

import com.campushub.entity.PaymentRecord;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentService {
    Map<String, Object> createPrepay(Long payerId, BigDecimal amount, int expireMinutes, String businessType, String businessTraceNo);
    void handlePaymentSuccess(Long paymentId, String tradeNo);
    void cancelWaitingPayment(Long paymentId);
    void refundPayment(Long paymentId);
    void settlePayment(Long paymentId, Long receiverId);
    PaymentRecord getPaymentRecord(Long paymentId);
    Map<String, Object> queryTransactions(Long userId, String type, int page, int pageSize);
}
