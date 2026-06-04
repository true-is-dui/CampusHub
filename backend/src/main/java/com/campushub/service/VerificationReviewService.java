package com.campushub.service;

import java.util.Map;

public interface VerificationReviewService {
    Map<String, Object> queryReviews(String status, int page, int pageSize);
    void approveReview(Long reviewId, Long adminId);
    void rejectReview(Long reviewId, Long adminId, String reason);
}
