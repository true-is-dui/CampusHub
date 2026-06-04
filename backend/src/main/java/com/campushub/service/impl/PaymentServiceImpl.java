package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.dto.PaymentRecordDTO;
import com.campushub.entity.PaymentRecord;
import com.campushub.entity.enums.BusinessType;
import com.campushub.entity.enums.PaymentStatus;
import com.campushub.mapper.PaymentRecordMapper;
import com.campushub.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRecordMapper paymentRecordMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Map<String, Object> createPrepay(Long payerId, BigDecimal amount,
                                            int expireMinutes, String businessType,
                                            String businessTraceNo) {
        PaymentRecord record = new PaymentRecord();
        record.setPayerId(payerId);
        record.setAmount(amount);
        record.setExpireAt(LocalDateTime.now().plusMinutes(expireMinutes));
        record.setBusinessType(BusinessType.valueOf(businessType));
        record.setBusinessTraceNo(businessTraceNo);
        record.setOutTradeNo(UUID.randomUUID().toString().replace("-", ""));
        record.setStatus(PaymentStatus.WAITING_PAY);
        paymentRecordMapper.insert(record);

        Map<String, Object> result = new HashMap<>();
        result.put("paymentId", record.getId());
        result.put("payEntry", "/api/payments/mock-pay/" + record.getId());
        return result;
    }

    public void handlePaymentSuccess(Long paymentId, String tradeNo) {
        PaymentRecord record = paymentRecordMapper.selectById(paymentId);
        if (record == null) {
            throw new BusinessException(40401, "支付记录不存在");
        }
        record.setStatus(PaymentStatus.PAID);
        record.setTradeNo(tradeNo);
        record.setStatusChangedAt(LocalDateTime.now());
        paymentRecordMapper.updateById(record);
    }

    public void cancelWaitingPayment(Long paymentId) {
        PaymentRecord record = paymentRecordMapper.selectById(paymentId);
        if (record == null) {
            throw new BusinessException(40401, "支付记录不存在");
        }
        if (record.getStatus() != PaymentStatus.WAITING_PAY) {
            throw new BusinessException(40001, "当前支付状态不可关闭");
        }
        record.setStatus(PaymentStatus.CLOSED);
        record.setCloseReason("用户取消或超时未支付");
        record.setStatusChangedAt(LocalDateTime.now());
        paymentRecordMapper.updateById(record);
    }

    public void refundPayment(Long paymentId) {
        PaymentRecord record = paymentRecordMapper.selectById(paymentId);
        if (record == null) {
            throw new BusinessException(40401, "支付记录不存在");
        }
        record.setStatus(PaymentStatus.REFUNDED);
        record.setStatusChangedAt(LocalDateTime.now());
        paymentRecordMapper.updateById(record);
    }

    public void settlePayment(Long paymentId, Long receiverId) {
        PaymentRecord record = paymentRecordMapper.selectById(paymentId);
        if (record == null) {
            throw new BusinessException(40401, "支付记录不存在");
        }
        record.setStatus(PaymentStatus.SETTLED);
        record.setReceiverId(receiverId);
        record.setStatusChangedAt(LocalDateTime.now());
        paymentRecordMapper.updateById(record);
    }

    public PaymentRecord getPaymentRecord(Long paymentId) {
        return paymentRecordMapper.selectById(paymentId);
    }

    public Map<String, Object> queryTransactions(Long userId, String type, int page, int pageSize) {
        Page<PaymentRecord> pageObj = new Page<>(page, pageSize);
        QueryWrapper<PaymentRecord> wrapper = new QueryWrapper<>();

        if ("PAID".equals(type) || "PAYER".equals(type)) {
            wrapper.eq("payer_id", userId);
        } else if ("RECEIVED".equals(type) || "RECEIVER".equals(type)) {
            wrapper.eq("receiver_id", userId);
        } else {
            wrapper.and(w -> w.eq("payer_id", userId).or().eq("receiver_id", userId));
        }
        wrapper.orderByDesc("created_at");

        IPage<PaymentRecord> result = paymentRecordMapper.selectPage(pageObj, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("list", result.getRecords().stream().map(this::toPaymentRecordDTO).toList());
        return data;
    }

    private PaymentRecordDTO toPaymentRecordDTO(PaymentRecord record) {
        PaymentRecordDTO dto = new PaymentRecordDTO();
        dto.setPaymentId(record.getId());
        dto.setPayerId(record.getPayerId());
        dto.setReceiverId(record.getReceiverId());
        dto.setBusinessType(record.getBusinessType() != null ? record.getBusinessType().name() : null);
        dto.setAmount(record.getAmount());
        dto.setStatus(record.getStatus() != null ? record.getStatus().name() : null);
        dto.setExpireAt(record.getExpireAt() != null ? record.getExpireAt().format(DT_FMT) : null);
        dto.setCloseReason(record.getCloseReason());
        dto.setStatusChangedAt(record.getStatusChangedAt() != null ? record.getStatusChangedAt().format(DT_FMT) : null);
        dto.setCreatedAt(record.getCreatedAt() != null ? record.getCreatedAt().format(DT_FMT) : null);
        return dto;
    }
}
