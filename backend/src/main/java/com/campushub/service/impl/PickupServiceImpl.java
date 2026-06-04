package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.dto.*;
import com.campushub.entity.PickupRequest;
import com.campushub.entity.User;
import com.campushub.entity.enums.*;
import com.campushub.mapper.PickupRequestMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.service.FileStorageService;
import com.campushub.service.PickupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PickupServiceImpl implements PickupService {

    private final PickupRequestMapper pickupRequestMapper;
    private final UserMapper userMapper;
    private final PaymentServiceImpl paymentService;
    private final FileStorageService fileStorageService;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public PickupCreateResultDTO publishPickup(PickupCreateRequest request, Long userId, Long credentialFileId) {
        RewardType rewardType = RewardType.valueOf(request.getRewardType());
        LocalDateTime acceptDeadline = LocalDateTime.parse(request.getAcceptDeadline(), DT_FMT);

        PickupRequest pickup = new PickupRequest();
        pickup.setPublisherId(userId);
        pickup.setCampus(request.getCampus());
        pickup.setPickupLocation(request.getPickupLocation());
        pickup.setDeliveryLocation(request.getDeliveryLocation());
        pickup.setItemDescription(request.getItemDescription());
        pickup.setRewardType(rewardType);
        pickup.setRewardAmount(request.getRewardAmount());
        pickup.setAcceptDeadline(acceptDeadline);
        pickup.setPickupCredentialFileId(credentialFileId);

        if (rewardType == RewardType.PAID) {
            pickup.setStatus(PickupStatus.WAITING_PAYMENT);
        } else {
            pickup.setStatus(PickupStatus.WAITING_ACCEPT);
        }

        pickupRequestMapper.insert(pickup);

        PickupCreateResultDTO result = new PickupCreateResultDTO();
        result.setPickupId(pickup.getId());
        result.setStatus(pickup.getStatus().name());

        if (rewardType == RewardType.PAID) {
            LocalDateTime paymentExpireAt = LocalDateTime.now().plusMinutes(3);
            Map<String, Object> prepayResult = paymentService.createPrepay(
                    userId,
                    request.getRewardAmount(),
                    3,
                    BusinessType.PICKUP_REQUEST.name(),
                    pickup.getId().toString()
            );
            Long paymentId = ((Number) prepayResult.get("paymentId")).longValue();
            pickup.setPaymentId(paymentId);
            pickupRequestMapper.updateById(pickup);

            result.setPayEntry((String) prepayResult.get("payEntry"));
            result.setExpireAt(paymentExpireAt.format(DT_FMT));
        }

        return result;
    }

    public Map<String, Object> queryHall(String campus, String rewardType, int page, int pageSize) {
        Page<PickupRequest> pageObj = new Page<>(page, pageSize);
        QueryWrapper<PickupRequest> wrapper = new QueryWrapper<>();
        wrapper.eq("status", PickupStatus.WAITING_ACCEPT);

        if (campus != null && !campus.isEmpty()) {
            wrapper.eq("campus", campus);
        }
        if (rewardType != null && !rewardType.isEmpty()) {
            wrapper.eq("reward_type", RewardType.valueOf(rewardType));
        }
        wrapper.orderByDesc("created_at");

        IPage<PickupRequest> result = pickupRequestMapper.selectPage(pageObj, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("list", result.getRecords().stream().map(this::toPickupSummaryDTO).toList());
        return data;
    }

    public PickupDetailDTO queryDetail(Long pickupId) {
        PickupRequest pickup = pickupRequestMapper.selectById(pickupId);
        if (pickup == null) {
            throw new BusinessException(40401, "代拿请求不存在");
        }

        PickupDetailDTO dto = new PickupDetailDTO();
        fillSummaryFields(dto, pickup);
        dto.setItemDescription(pickup.getItemDescription());

        if (pickup.getAcceptorId() != null) {
            User acceptor = userMapper.selectById(pickup.getAcceptorId());
            if (acceptor != null) {
                UserSummaryDTO acceptorDTO = new UserSummaryDTO();
                acceptorDTO.setUserId(acceptor.getId());
                acceptorDTO.setNickname(acceptor.getNickname());
                dto.setAcceptor(acceptorDTO);
            }
        }
        dto.setAcceptedAt(pickup.getAcceptedAt() != null ? pickup.getAcceptedAt().format(DT_FMT) : null);

        return dto;
    }

    public PickupPaymentResultDTO getPaymentEntry(Long pickupId, Long userId) {
        PickupRequest pickup = pickupRequestMapper.selectById(pickupId);
        if (pickup == null) {
            throw new BusinessException(40401, "代拿请求不存在");
        }
        if (!userId.equals(pickup.getPublisherId())) {
            throw new BusinessException(40301, "只有发布者可以查看支付入口");
        }
        if (pickup.getPaymentId() == null) {
            throw new BusinessException(40001, "该代拿请求无需支付");
        }

        PickupPaymentResultDTO dto = new PickupPaymentResultDTO();
        dto.setPayEntry("/api/payments/mock-pay/" + pickup.getPaymentId());
        dto.setExpireAt(pickup.getAcceptDeadline() != null ? pickup.getAcceptDeadline().format(DT_FMT) : null);
        return dto;
    }

    @Transactional
    public PickupAcceptResultDTO acceptPickup(Long pickupId, Long acceptorId) {
        PickupRequest pickup = pickupRequestMapper.selectById(pickupId);
        if (pickup == null) {
            throw new BusinessException(40401, "代拿请求不存在");
        }
        if (pickup.getStatus() != PickupStatus.WAITING_ACCEPT) {
            throw new BusinessException(40001, "该代拿请求当前不可接单");
        }
        if (pickup.getPublisherId().equals(acceptorId)) {
            throw new BusinessException(40002, "不能接受自己发布的代拿请求");
        }

        pickup.setAcceptorId(acceptorId);
        pickup.setStatus(PickupStatus.IN_PROGRESS);
        pickup.setAcceptedAt(LocalDateTime.now());
        pickupRequestMapper.updateById(pickup);

        PickupAcceptResultDTO result = new PickupAcceptResultDTO();
        result.setStatus(pickup.getStatus().name());
        result.setAcceptedAt(pickup.getAcceptedAt().format(DT_FMT));
        return result;
    }

    public void uploadCompletionProof(Long pickupId, Long acceptorId, Long fileId) {
        PickupRequest pickup = pickupRequestMapper.selectById(pickupId);
        if (pickup == null) {
            throw new BusinessException(40401, "代拿请求不存在");
        }
        if (!acceptorId.equals(pickup.getAcceptorId())) {
            throw new BusinessException(40301, "只有接单者可以上传完成凭证");
        }
        pickup.setCompletionProofFileId(fileId);
        pickupRequestMapper.updateById(pickup);
    }

    @Transactional
    public Map<String, Object> confirmComplete(Long pickupId, Long publisherId) {
        PickupRequest pickup = pickupRequestMapper.selectById(pickupId);
        if (pickup == null) {
            throw new BusinessException(40401, "代拿请求不存在");
        }
        if (!publisherId.equals(pickup.getPublisherId())) {
            throw new BusinessException(40301, "只有发布者可以确认完成");
        }
        if (pickup.getCompletionProofFileId() == null) {
            throw new BusinessException(40001, "完成凭证尚未上传");
        }

        pickup.setStatus(PickupStatus.COMPLETED);
        pickup.setCompletedAt(LocalDateTime.now());
        pickupRequestMapper.updateById(pickup);

        if (pickup.getRewardType() == RewardType.PAID && pickup.getPaymentId() != null) {
            paymentService.settlePayment(pickup.getPaymentId(), pickup.getAcceptorId());
        }

        return Map.of("status", "COMPLETED");
    }

    @Transactional
    public Map<String, Object> cancelPickup(Long pickupId, Long publisherId, String reason) {
        PickupRequest pickup = pickupRequestMapper.selectById(pickupId);
        if (pickup == null) {
            throw new BusinessException(40401, "代拿请求不存在");
        }
        if (!publisherId.equals(pickup.getPublisherId())) {
            throw new BusinessException(40301, "只有发布者可以取消");
        }
        if (pickup.getStatus() != PickupStatus.WAITING_PAYMENT && pickup.getStatus() != PickupStatus.WAITING_ACCEPT) {
            throw new BusinessException(40001, "当前状态不允许取消");
        }

        if (pickup.getStatus() == PickupStatus.WAITING_PAYMENT && pickup.getPaymentId() != null) {
            paymentService.cancelWaitingPayment(pickup.getPaymentId());
        } else if (pickup.getStatus() == PickupStatus.WAITING_ACCEPT
                && pickup.getRewardType() == RewardType.PAID
                && pickup.getPaymentId() != null) {
            paymentService.refundPayment(pickup.getPaymentId());
        }

        pickup.setStatus(PickupStatus.CANCELLED);
        try {
            pickup.setCancelReason(PickupCancelReason.valueOf(reason));
        } catch (IllegalArgumentException e) {
            pickup.setCancelReason(PickupCancelReason.USER_CANCELLED);
        }
        pickupRequestMapper.updateById(pickup);

        return Map.of("status", "CANCELLED");
    }

    public Map<String, Object> queryMyPublished(Long userId, String status, int page, int pageSize) {
        Page<PickupRequest> pageObj = new Page<>(page, pageSize);
        QueryWrapper<PickupRequest> wrapper = new QueryWrapper<>();
        wrapper.eq("publisher_id", userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", PickupStatus.valueOf(status));
        }
        wrapper.orderByDesc("created_at");

        IPage<PickupRequest> result = pickupRequestMapper.selectPage(pageObj, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("list", result.getRecords().stream().map(this::toPickupSummaryDTO).toList());
        return data;
    }

    public Map<String, Object> queryMyAccepted(Long userId, String status, int page, int pageSize) {
        Page<PickupRequest> pageObj = new Page<>(page, pageSize);
        QueryWrapper<PickupRequest> wrapper = new QueryWrapper<>();
        wrapper.eq("acceptor_id", userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", PickupStatus.valueOf(status));
        }
        wrapper.orderByDesc("created_at");

        IPage<PickupRequest> result = pickupRequestMapper.selectPage(pageObj, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("list", result.getRecords().stream().map(this::toPickupSummaryDTO).toList());
        return data;
    }

    public byte[] loadCredential(Long pickupId, Long userId) {
        PickupRequest pickup = pickupRequestMapper.selectById(pickupId);
        if (pickup == null || pickup.getPickupCredentialFileId() == null) {
            throw new BusinessException(40401, "取件凭证不存在");
        }
        // Both publisher and acceptor can view credential
        if (!userId.equals(pickup.getPublisherId()) && !userId.equals(pickup.getAcceptorId())) {
            throw new BusinessException(40301, "无权查看取件凭证");
        }
        Map<String, Object> fileData = fileStorageService.loadFile(pickup.getPickupCredentialFileId());
        return (byte[]) fileData.get("bytes");
    }

    public String getCredentialMimeType(Long pickupId) {
        PickupRequest pickup = pickupRequestMapper.selectById(pickupId);
        if (pickup == null || pickup.getPickupCredentialFileId() == null) {
            return null;
        }
        Map<String, Object> fileData = fileStorageService.loadFile(pickup.getPickupCredentialFileId());
        return (String) fileData.get("mimeType");
    }

    public byte[] loadCompletionProof(Long pickupId, Long userId) {
        PickupRequest pickup = pickupRequestMapper.selectById(pickupId);
        if (pickup == null || pickup.getCompletionProofFileId() == null) {
            throw new BusinessException(40401, "完成凭证不存在");
        }
        if (!userId.equals(pickup.getPublisherId()) && !userId.equals(pickup.getAcceptorId())) {
            throw new BusinessException(40301, "无权查看完成凭证");
        }
        Map<String, Object> fileData = fileStorageService.loadFile(pickup.getCompletionProofFileId());
        return (byte[]) fileData.get("bytes");
    }

    public String getCompletionProofMimeType(Long pickupId) {
        PickupRequest pickup = pickupRequestMapper.selectById(pickupId);
        if (pickup == null || pickup.getCompletionProofFileId() == null) {
            return null;
        }
        Map<String, Object> fileData = fileStorageService.loadFile(pickup.getCompletionProofFileId());
        return (String) fileData.get("mimeType");
    }

    private PickupSummaryDTO toPickupSummaryDTO(PickupRequest pickup) {
        PickupSummaryDTO dto = new PickupSummaryDTO();
        fillSummaryFields(dto, pickup);
        return dto;
    }

    private void fillSummaryFields(PickupSummaryDTO dto, PickupRequest pickup) {
        dto.setPickupId(pickup.getId());
        dto.setCampus(pickup.getCampus());
        dto.setPickupLocation(pickup.getPickupLocation());
        dto.setDeliveryLocation(pickup.getDeliveryLocation());

        String desc = pickup.getItemDescription();
        dto.setItemDescriptionPreview(desc != null && desc.length() > 50 ? desc.substring(0, 50) + "..." : desc);

        dto.setRewardType(pickup.getRewardType() != null ? pickup.getRewardType().name() : null);
        dto.setRewardAmount(pickup.getRewardAmount());
        dto.setStatus(pickup.getStatus() != null ? pickup.getStatus().name() : null);
        dto.setCancelReason(pickup.getCancelReason() != null ? pickup.getCancelReason().name() : null);
        dto.setAcceptDeadline(pickup.getAcceptDeadline() != null ? pickup.getAcceptDeadline().format(DT_FMT) : null);
        dto.setCreatedAt(pickup.getCreatedAt() != null ? pickup.getCreatedAt().format(DT_FMT) : null);
        dto.setCompletedAt(pickup.getCompletedAt() != null ? pickup.getCompletedAt().format(DT_FMT) : null);

        if (pickup.getPublisherId() != null) {
            User publisher = userMapper.selectById(pickup.getPublisherId());
            if (publisher != null) {
                UserSummaryDTO publisherDTO = new UserSummaryDTO();
                publisherDTO.setUserId(publisher.getId());
                publisherDTO.setNickname(publisher.getNickname());
                dto.setPublisher(publisherDTO);
            }
        }
    }
}
