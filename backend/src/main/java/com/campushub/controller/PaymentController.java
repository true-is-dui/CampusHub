package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import com.campushub.config.AuthInterceptor;
import com.campushub.config.CurrentUserContext;
import com.campushub.entity.PaymentRecord;
import com.campushub.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/alipay/notify")
    public String alipayNotify(
            @RequestParam("out_trade_no") String outTradeNo,
            @RequestParam("trade_no") String tradeNo,
            @RequestParam("trade_status") String tradeStatus
    ) {
        // In real implementation, find by outTradeNo and handle payment success
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            // paymentService.handlePaymentSuccess(record.getId(), tradeNo);
        }
        return "success";
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<Map<String, Object>> getPaymentRecord(
            HttpServletRequest request,
            @PathVariable Long paymentId
    ) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        PaymentRecord record = paymentService.getPaymentRecord(paymentId);
        if (record == null) {
            throw new BusinessException(40401, "RESOURCE_NOT_FOUND");
        }
        if (!record.getPayerId().equals(ctx.getCurrentUserId()) &&
            (record.getReceiverId() == null || !record.getReceiverId().equals(ctx.getCurrentUserId()))) {
            throw new BusinessException(40301, "AUTH_STATUS_NOT_ALLOWED");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", record.getId());
        data.put("payerId", record.getPayerId());
        data.put("receiverId", record.getReceiverId());
        data.put("businessType", record.getBusinessType() != null ? record.getBusinessType().name() : null);
        data.put("amount", record.getAmount());
        data.put("status", record.getStatus() != null ? record.getStatus().name() : null);
        data.put("expireAt", record.getExpireAt());
        data.put("closeReason", record.getCloseReason());
        data.put("statusChangedAt", record.getStatusChangedAt());
        data.put("createdAt", record.getCreatedAt());
        return ApiResponse.ok(data);
    }
}
