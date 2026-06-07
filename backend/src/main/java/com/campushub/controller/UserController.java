package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.CurrentUserContext;
import com.campushub.dto.user.UpdateProfileRequest;
import com.campushub.dto.user.UserMeResponse;
import com.campushub.dto.user.VerificationSubmitRequest;
import com.campushub.dto.user.VerificationSubmitResponse;
import com.campushub.security.CurrentUser;
import com.campushub.service.dto.StoredFileContent;
import com.campushub.service.UserService;
import com.campushub.service.VerificationReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 当前用户接口。需登录访问，身份由 {@code @CurrentUser} 从 JWT 解析注入，
 * 不接受前端传入的用户标识。
 *
 * <p>multipart 表单的文本字段封装为请求 DTO，用 {@code @Valid @ModelAttribute} 在入口
 * 做声明式格式校验（与注册/登录一致）；文件作为独立 {@code @RequestPart} 接收。
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final VerificationReviewService verificationReviewService;

    /** 获取当前登录用户的完整资料（读库返回最新值）。 */
    @GetMapping("/me")
    public ApiResponse<UserMeResponse> getCurrentUser(@CurrentUser CurrentUserContext me) {
        return ApiResponse.ok(userService.getCurrentUser(me.getCurrentUserId()));
    }

    /** 编辑个人资料，支持部分更新和可选头像上传。 */
    @PutMapping(value = "/me/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> updateProfile(@CurrentUser CurrentUserContext me,
                                           @Valid @ModelAttribute UpdateProfileRequest request,
                                           @RequestPart(required = false) MultipartFile avatar) {
        userService.updateProfile(me.getCurrentUserId(), request.getNickname(), avatar,
                request.getCollege(), request.getContact());
        return ApiResponse.ok();
    }

    /** 提交实名认证申请。 */
    @PostMapping(value = "/me/verification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<VerificationSubmitResponse> submitVerification(@CurrentUser CurrentUserContext me,
                                                                      @Valid @ModelAttribute VerificationSubmitRequest request,
                                                                      @RequestPart MultipartFile verificationImage) {
        return ApiResponse.ok(verificationReviewService.submitVerification(
                me.getCurrentUserId(), request.getStudentId(), request.getRealName(), verificationImage));
    }

    /** 公开读取用户头像。 */
    @GetMapping("/{userId}/avatar")
    public ResponseEntity<Resource> getAvatar(@PathVariable Long userId) {
        StoredFileContent content = userService.loadAvatar(userId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(content.getMimeType()))
            .contentLength(content.getFileSize())
            .body(content.getResource());
    }
}
