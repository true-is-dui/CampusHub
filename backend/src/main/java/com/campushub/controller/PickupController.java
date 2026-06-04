package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import com.campushub.config.AuthInterceptor;
import com.campushub.config.CurrentUserContext;
import com.campushub.dto.*;
import com.campushub.entity.enums.FileUsage;
import com.campushub.service.FileStorageService;
import com.campushub.service.PickupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@RestController
@RequestMapping("/pickup-requests")
@RequiredArgsConstructor
public class PickupController {

    private final PickupService pickupService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ApiResponse<Map<String, Object>> queryHall(
            @RequestParam(value = "campus", required = false) String campus,
            @RequestParam(value = "rewardType", required = false) String rewardType,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(pickupService.queryHall(campus, rewardType, page, pageSize));
    }

    @PostMapping
    public ApiResponse<PickupCreateResultDTO> publishPickup(
            HttpServletRequest request,
            @RequestParam("campus") String campus,
            @RequestParam("pickupLocation") String pickupLocation,
            @RequestParam("deliveryLocation") String deliveryLocation,
            @RequestParam("itemDescription") String itemDescription,
            @RequestParam(value = "pickupCredential", required = false) MultipartFile pickupCredential,
            @RequestParam("rewardType") String rewardType,
            @RequestParam(value = "rewardAmount", required = false) String rewardAmount,
            @RequestParam("acceptDeadline") String acceptDeadline
    ) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        if (ctx.getAuthStatus() != com.campushub.entity.enums.AuthStatus.APPROVED) {
            throw new BusinessException(40301, "AUTH_STATUS_NOT_ALLOWED");
        }

        Long credentialFileId = null;
        if (pickupCredential != null && !pickupCredential.isEmpty()) {
            credentialFileId = fileStorageService.uploadImage(pickupCredential, ctx.getCurrentUserId(), FileUsage.PICKUP_CREDENTIAL);
        }

        PickupCreateRequest createRequest = new PickupCreateRequest();
        createRequest.setCampus(campus);
        createRequest.setPickupLocation(pickupLocation);
        createRequest.setDeliveryLocation(deliveryLocation);
        createRequest.setItemDescription(itemDescription);
        createRequest.setRewardType(rewardType);
        if (rewardAmount != null && !rewardAmount.isEmpty()) {
            createRequest.setRewardAmount(new java.math.BigDecimal(rewardAmount));
        }
        createRequest.setAcceptDeadline(acceptDeadline);

        return ApiResponse.ok(pickupService.publishPickup(createRequest, ctx.getCurrentUserId(), credentialFileId));
    }

    @GetMapping("/{pickupId}")
    public ApiResponse<PickupDetailDTO> queryDetail(@PathVariable Long pickupId) {
        return ApiResponse.ok(pickupService.queryDetail(pickupId));
    }

    @GetMapping("/{pickupId}/credential")
    public void getCredential(
            HttpServletRequest request,
            @PathVariable Long pickupId,
            HttpServletResponse response
    ) throws IOException {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        byte[] imageBytes = pickupService.loadCredential(pickupId, ctx.getCurrentUserId());
        String mimeType = pickupService.getCredentialMimeType(pickupId);
        response.setContentType(mimeType != null ? mimeType : "image/jpeg");
        response.setContentLength(imageBytes.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(imageBytes);
        }
    }

    @GetMapping("/{pickupId}/payment-entry")
    public ApiResponse<PickupPaymentResultDTO> getPaymentEntry(
            HttpServletRequest request,
            @PathVariable Long pickupId
    ) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        return ApiResponse.ok(pickupService.getPaymentEntry(pickupId, ctx.getCurrentUserId()));
    }

    @PostMapping("/{pickupId}/accept")
    public ApiResponse<PickupAcceptResultDTO> acceptPickup(
            HttpServletRequest request,
            @PathVariable Long pickupId
    ) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        if (ctx.getAuthStatus() != com.campushub.entity.enums.AuthStatus.APPROVED) {
            throw new BusinessException(40301, "AUTH_STATUS_NOT_ALLOWED");
        }
        return ApiResponse.ok(pickupService.acceptPickup(pickupId, ctx.getCurrentUserId()));
    }

    @PostMapping("/{pickupId}/completion-proof")
    public ApiResponse<Void> uploadCompletionProof(
            HttpServletRequest request,
            @PathVariable Long pickupId,
            @RequestParam("proofImage") MultipartFile proofImage
    ) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        Long fileId = fileStorageService.uploadImage(proofImage, ctx.getCurrentUserId(), FileUsage.COMPLETION_PROOF);
        pickupService.uploadCompletionProof(pickupId, ctx.getCurrentUserId(), fileId);
        return ApiResponse.ok();
    }

    @GetMapping("/{pickupId}/completion-proof")
    public void getCompletionProof(
            HttpServletRequest request,
            @PathVariable Long pickupId,
            HttpServletResponse response
    ) throws IOException {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        byte[] imageBytes = pickupService.loadCompletionProof(pickupId, ctx.getCurrentUserId());
        String mimeType = pickupService.getCompletionProofMimeType(pickupId);
        response.setContentType(mimeType != null ? mimeType : "image/jpeg");
        response.setContentLength(imageBytes.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(imageBytes);
        }
    }

    @PostMapping("/{pickupId}/completion-confirmation")
    public ApiResponse<Map<String, Object>> confirmComplete(
            HttpServletRequest request,
            @PathVariable Long pickupId
    ) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        return ApiResponse.ok(pickupService.confirmComplete(pickupId, ctx.getCurrentUserId()));
    }

    @PostMapping("/{pickupId}/cancel")
    public ApiResponse<Map<String, Object>> cancelPickup(
            HttpServletRequest request,
            @PathVariable Long pickupId,
            @RequestBody(required = false) ReasonRequest reasonRequest
    ) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        String reason = reasonRequest != null ? reasonRequest.getReason() : null;
        return ApiResponse.ok(pickupService.cancelPickup(pickupId, ctx.getCurrentUserId(), reason));
    }
}
