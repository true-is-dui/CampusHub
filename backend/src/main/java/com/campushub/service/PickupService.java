package com.campushub.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campushub.dto.*;

import java.util.Map;

public interface PickupService {
    PickupCreateResultDTO publishPickup(PickupCreateRequest request, Long userId, Long credentialFileId);
    Map<String, Object> queryHall(String campus, String rewardType, int page, int pageSize);
    PickupDetailDTO queryDetail(Long pickupId);
    PickupPaymentResultDTO getPaymentEntry(Long pickupId, Long userId);
    PickupAcceptResultDTO acceptPickup(Long pickupId, Long acceptorId);
    void uploadCompletionProof(Long pickupId, Long acceptorId, Long fileId);
    Map<String, Object> confirmComplete(Long pickupId, Long publisherId);
    Map<String, Object> cancelPickup(Long pickupId, Long publisherId, String reason);
    Map<String, Object> queryMyPublished(Long userId, String status, int page, int pageSize);
    Map<String, Object> queryMyAccepted(Long userId, String status, int page, int pageSize);
    byte[] loadCredential(Long pickupId, Long userId);
    String getCredentialMimeType(Long pickupId);
    byte[] loadCompletionProof(Long pickupId, Long userId);
    String getCompletionProofMimeType(Long pickupId);
}
