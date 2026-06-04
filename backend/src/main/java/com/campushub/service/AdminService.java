package com.campushub.service;

import com.campushub.dto.AdminHandleRequest;
import com.campushub.dto.VerificationReviewDTO;

import java.util.Map;

public interface AdminService {
    Map<String, Object> queryVerificationReviews(String status, int page, int pageSize);
    void handleVerificationReview(Long adminId, Long reviewId, AdminHandleRequest request);
    byte[] loadVerificationImage(Long reviewId);
}
