package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import com.campushub.config.AuthInterceptor;
import com.campushub.config.CurrentUserContext;
import com.campushub.dto.AdminHandleRequest;
import com.campushub.entity.enums.UserRole;
import com.campushub.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/verification-reviews")
    public ApiResponse<Map<String, Object>> getVerificationReviews(
            HttpServletRequest request,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        checkAdmin(request);
        return ApiResponse.ok(adminService.queryVerificationReviews(status, page, pageSize));
    }

    @PostMapping("/verification-reviews/{reviewId}/handle")
    public ApiResponse<Void> handleVerificationReview(
            HttpServletRequest request,
            @PathVariable Long reviewId,
            @RequestBody AdminHandleRequest handleRequest
    ) {
        CurrentUserContext ctx = checkAdmin(request);
        adminService.handleVerificationReview(ctx.getCurrentUserId(), reviewId, handleRequest);
        return ApiResponse.ok();
    }

    @GetMapping("/verification-reviews/{reviewId}/image")
    public void getVerificationImage(
            HttpServletRequest request,
            @PathVariable Long reviewId,
            HttpServletResponse response
    ) throws IOException {
        checkAdmin(request);
        byte[] imageBytes = adminService.loadVerificationImage(reviewId);
        response.setContentType("image/jpeg");
        response.setContentLength(imageBytes.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(imageBytes);
        }
    }

    private CurrentUserContext checkAdmin(HttpServletRequest request) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        if (ctx.getRole() != UserRole.ADMIN) {
            throw new BusinessException(40301, "ADMIN_REQUIRED");
        }
        return ctx;
    }
}
