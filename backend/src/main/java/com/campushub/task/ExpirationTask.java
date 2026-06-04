package com.campushub.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campushub.entity.PickupRequest;
import com.campushub.entity.PaymentRecord;
import com.campushub.entity.enums.*;
import com.campushub.mapper.PickupRequestMapper;
import com.campushub.mapper.PaymentRecordMapper;
import com.campushub.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpirationTask {

    private final PickupRequestMapper pickupRequestMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final NotificationServiceImpl notificationService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireAcceptDeadlines() {
        QueryWrapper<PickupRequest> wrapper = new QueryWrapper<>();
        wrapper.eq("status", PickupStatus.WAITING_ACCEPT)
               .isNotNull("accept_deadline")
               .lt("accept_deadline", LocalDateTime.now());
        List<PickupRequest> expired = pickupRequestMapper.selectList(wrapper);

        for (PickupRequest pickup : expired) {
            pickup.setStatus(PickupStatus.CANCELLED);
            pickup.setCancelReason(PickupCancelReason.ACCEPT_DEADLINE_EXPIRED);
            pickupRequestMapper.updateById(pickup);

            notificationService.createNotice(
                    pickup.getPublisherId(),
                    NotificationType.PICKUP,
                    "代取请求已过期",
                    "您发布的代取请求（" + pickup.getPickupLocation() + " → " + pickup.getDeliveryLocation() + "）已超过接单截止时间，系统已自动取消。",
                    BusinessType.PICKUP_REQUEST,
                    pickup.getId()
            );
            log.info("Expired pickup {} due to accept deadline", pickup.getId());
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirePayments() {
        QueryWrapper<PaymentRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("status", PaymentStatus.WAITING_PAY)
               .isNotNull("expire_at")
               .lt("expire_at", LocalDateTime.now());
        List<PaymentRecord> expired = paymentRecordMapper.selectList(wrapper);

        for (PaymentRecord payment : expired) {
            payment.setStatus(PaymentStatus.CLOSED);
            payment.setCloseReason("支付超时未完成");
            payment.setStatusChangedAt(LocalDateTime.now());
            paymentRecordMapper.updateById(payment);

            if (payment.getBusinessType() == BusinessType.PICKUP_REQUEST && payment.getBusinessTraceNo() != null) {
                try {
                    Long pickupId = Long.parseLong(payment.getBusinessTraceNo());
                    PickupRequest pickup = pickupRequestMapper.selectById(pickupId);
                    if (pickup != null && pickup.getStatus() == PickupStatus.WAITING_PAYMENT) {
                        pickup.setStatus(PickupStatus.CANCELLED);
                        pickup.setCancelReason(PickupCancelReason.PAYMENT_EXPIRED);
                        pickupRequestMapper.updateById(pickup);

                        notificationService.createNotice(
                                pickup.getPublisherId(),
                                NotificationType.PAYMENT,
                                "支付已超时",
                                "您发布的代取请求（" + pickup.getPickupLocation() + " → " + pickup.getDeliveryLocation() + "）因支付超时已被系统取消。",
                                BusinessType.PICKUP_REQUEST,
                                pickup.getId()
                        );
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid businessTraceNo for payment {}: {}", payment.getId(), payment.getBusinessTraceNo());
                }
            }
            log.info("Expired payment {} due to timeout", payment.getId());
        }
    }
}
