package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.config.AuthInterceptor;
import com.campushub.config.CurrentUserContext;
import com.campushub.dto.*;
import com.campushub.entity.enums.FileUsage;
import com.campushub.service.EvaluationService;
import com.campushub.service.FileStorageService;
import com.campushub.service.PaymentService;
import com.campushub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final EvaluationService evaluationService;
    private final PaymentService paymentService;

    @GetMapping("/me")
    public ApiResponse<UserMeDTO> getCurrentUser(HttpServletRequest request) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        return ApiResponse.ok(userService.getCurrentUser(ctx.getCurrentUserId()));
    }

    @PutMapping("/me")
    public ApiResponse<Void> updateProfile(
            HttpServletRequest request,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "college", required = false) String college,
            @RequestParam(value = "contact", required = false) String contact,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar
    ) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        if (avatar != null && !avatar.isEmpty()) {
            Long avatarFileId = fileStorageService.uploadImage(avatar, ctx.getCurrentUserId(), FileUsage.AVATAR);
            userService.updateAvatarFileId(ctx.getCurrentUserId(), avatarFileId);
        }
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setNickname(nickname);
        req.setCollege(college);
        req.setContact(contact);
        userService.updateProfile(ctx.getCurrentUserId(), req);
        return ApiResponse.ok();
    }

    @PostMapping("/me/verification")
    public ApiResponse<Void> submitVerification(
            HttpServletRequest request,
            @RequestParam("studentId") String studentId,
            @RequestParam("realName") String realName,
            @RequestParam("verificationImage") MultipartFile verificationImage
    ) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        Long fileId = fileStorageService.uploadImage(verificationImage, ctx.getCurrentUserId(), FileUsage.VERIFICATION_MATERIAL);
        userService.submitVerification(ctx.getCurrentUserId(), studentId, realName, fileId);
        return ApiResponse.ok();
    }

    @GetMapping("/{userId}/profile")
    public ApiResponse<UserPublicProfileDTO> getUserProfile(
            @PathVariable Long userId,
            @RequestParam(value = "includeRating", required = false, defaultValue = "false") boolean includeRating
    ) {
        UserPublicProfileDTO profile = new UserPublicProfileDTO();
        UserSummaryDTO summary = userService.getUserSummary(userId);
        profile.setNickname(summary.getNickname());
        if (includeRating) {
            profile.setRatingSummary(evaluationService.queryUserRatingSummary(userId));
        }
        return ApiResponse.ok(profile);
    }

    @GetMapping("/{userId}/avatar")
    public void loadAvatar(@PathVariable Long userId, HttpServletResponse response) throws IOException {
        Map<String, Object> avatarData = userService.loadAvatar(userId);
        byte[] imageBytes = (byte[]) avatarData.get("bytes");
        String mimeType = (String) avatarData.get("mimeType");
        response.setContentType(mimeType != null ? mimeType : "image/jpeg");
        response.setContentLength(imageBytes.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(imageBytes);
        }
    }

    @GetMapping("/{userId}/rating-summary")
    public ApiResponse<RatingSummaryDTO> getUserRatingSummary(@PathVariable Long userId) {
        return ApiResponse.ok(evaluationService.queryUserRatingSummary(userId));
    }

    @GetMapping("/{userId}/evaluations")
    public ApiResponse<Map<String, Object>> getUserEvaluations(
            @PathVariable Long userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(evaluationService.queryUserEvaluations(userId, page, pageSize));
    }

    @GetMapping("/me/transactions")
    public ApiResponse<Map<String, Object>> getMyTransactions(
            HttpServletRequest request,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        return ApiResponse.ok(paymentService.queryTransactions(ctx.getCurrentUserId(), type, page, pageSize));
    }
}
