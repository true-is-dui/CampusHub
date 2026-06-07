package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.CurrentUserContext;
import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.user.AdminHandleRequest;
import com.campushub.dto.user.VerificationReviewSummary;
import com.campushub.entity.enums.ReviewStatus;
import com.campushub.security.CurrentUser;
import com.campushub.service.dto.StoredFileContent;
import com.campushub.service.VerificationReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理员实名认证审核接口。 */
@Validated
@RestController
@RequestMapping("/admin/verification-reviews")
@RequiredArgsConstructor
public class AdminVerificationReviewController {

    private final VerificationReviewService verificationReviewService;

    @GetMapping
    public ApiResponse<PageResult<VerificationReviewSummary>> queryReviews(
            @CurrentUser CurrentUserContext admin,
            @RequestParam(required = false) ReviewStatus status,
            @Valid PageQuery pageQuery) {
        return ApiResponse.ok(verificationReviewService.queryReviews(admin, status, pageQuery));
    }

    @PostMapping("/{reviewId}/handle")
    public ApiResponse<Void> handleReview(@CurrentUser CurrentUserContext admin,
                                          @PathVariable Long reviewId,
                                          @Valid @RequestBody AdminHandleRequest request) {
        verificationReviewService.handleReview(admin, reviewId, request);
        return ApiResponse.ok();
    }

    @GetMapping("/{reviewId}/image")
    public ResponseEntity<Resource> getReviewImage(@CurrentUser CurrentUserContext admin,
                                                   @PathVariable Long reviewId) {
        StoredFileContent content = verificationReviewService.loadReviewImage(admin, reviewId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.getMimeType()))
                .contentLength(content.getFileSize())
                .body(content.getResource());
    }
}
