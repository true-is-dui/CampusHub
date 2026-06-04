package com.campushub.service;

import com.campushub.dto.*;

import java.util.Map;

public interface EvaluationService {
    EvaluationEligibilityDTO queryEvaluationEligibility(Long pickupId, Long reviewerId);
    Long submitEvaluation(Long pickupId, Long reviewerId, String ratingLevel, String content);
    RatingSummaryDTO queryUserRatingSummary(Long userId);
    Map<String, Object> queryUserEvaluations(Long userId, int page, int pageSize);
}
