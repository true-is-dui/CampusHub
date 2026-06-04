package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import com.campushub.config.AuthInterceptor;
import com.campushub.config.CurrentUserContext;
import com.campushub.dto.EvaluationCreateRequest;
import com.campushub.dto.EvaluationEligibilityDTO;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.service.EvaluationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping("/pickup-requests/{pickupId}/evaluations")
    public ApiResponse<Map<String, Object>> createEvaluation(
            HttpServletRequest request,
            @PathVariable Long pickupId,
            @RequestBody EvaluationCreateRequest createRequest
    ) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        if (ctx.getAuthStatus() != AuthStatus.APPROVED) {
            throw new BusinessException(40301, "AUTH_STATUS_NOT_ALLOWED");
        }
        Long evaluationId = evaluationService.submitEvaluation(
                pickupId, ctx.getCurrentUserId(), createRequest.getRatingLevel(), createRequest.getContent());
        return ApiResponse.ok(Map.of("evaluationId", evaluationId));
    }

    @GetMapping("/pickup-requests/{pickupId}/evaluation-eligibility")
    public ApiResponse<EvaluationEligibilityDTO> checkEvaluationEligibility(
            HttpServletRequest request,
            @PathVariable Long pickupId
    ) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        return ApiResponse.ok(evaluationService.queryEvaluationEligibility(pickupId, ctx.getCurrentUserId()));
    }
}
