package com.campushub.controller;

import com.campushub.common.CurrentUserContext;
import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.user.VerificationReviewSummary;
import com.campushub.dto.user.VerificationSubmitResponse;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.ReviewStatus;
import com.campushub.entity.enums.UserRole;
import com.campushub.security.JwtUtil;
import com.campushub.service.dto.StoredFileContent;
import com.campushub.service.UserService;
import com.campushub.service.VerificationReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class, AdminVerificationReviewController.class})
class UserFileAndVerificationWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private VerificationReviewService verificationReviewService;
    @MockitoBean
    private com.campushub.service.PickupService pickupService;
    @MockitoBean
    private com.campushub.service.NotificationService notificationService;
    @MockitoBean
    private com.campushub.service.EvaluationService evaluationService;
    @MockitoBean
    private com.campushub.service.PointService pointService;
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void updateProfile_acceptsMultipartPut() throws Exception {
        when(jwtUtil.parse("VALID"))
                .thenReturn(new CurrentUserContext(7L, UserRole.USER, AuthStatus.APPROVED));
        MockMultipartFile avatar = new MockMultipartFile(
                "avatar", "avatar.png", "image/png", new byte[]{1});
        MockMultipartHttpServletRequestBuilder request = multipart("/users/me/profile");
        request.file(avatar)
                .param("nickname", "alice")
                .header("Authorization", "Bearer VALID")
                .with(servletRequest -> {
                    servletRequest.setMethod("PUT");
                    return servletRequest;
                });

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void submitVerification_returnsReviewing() throws Exception {
        when(jwtUtil.parse("VALID"))
                .thenReturn(new CurrentUserContext(7L, UserRole.USER, AuthStatus.UNVERIFIED));
        when(verificationReviewService.submitVerification(eq(7L), eq("20260001"), eq("张三"), any()))
                .thenReturn(new VerificationSubmitResponse(AuthStatus.REVIEWING));
        MockMultipartFile image = new MockMultipartFile(
                "verificationImage", "card.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/users/me/verification")
                        .file(image)
                        .param("studentId", "20260001")
                        .param("realName", "张三")
                        .header("Authorization", "Bearer VALID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authStatus").value("REVIEWING"));
    }

    @Test
    void avatar_isPublicAndReturnsImageContent() throws Exception {
        when(userService.loadAvatar(7L)).thenReturn(new StoredFileContent(
                new ByteArrayResource(new byte[]{1, 2, 3}), "image/png", 3L));

        mockMvc.perform(get("/users/7/avatar"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void adminCanQueryVerificationReviews() throws Exception {
        when(jwtUtil.parse("ADMIN"))
                .thenReturn(new CurrentUserContext(99L, UserRole.ADMIN, AuthStatus.APPROVED));
        when(verificationReviewService.queryReviews(any(), any(), any(PageQuery.class)))
                .thenReturn(PageResult.of(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<VerificationReviewSummary>(1, 20, 0), List.of()));

        mockMvc.perform(get("/admin/verification-reviews")
                        .header("Authorization", "Bearer ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    void adminHandleRejectValidatesBody() throws Exception {
        when(jwtUtil.parse("ADMIN"))
                .thenReturn(new CurrentUserContext(99L, UserRole.ADMIN, AuthStatus.APPROVED));

        mockMvc.perform(post("/admin/verification-reviews/1/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void adminHandleRejectWithoutReason_rejectedWith40001() throws Exception {
        when(jwtUtil.parse("ADMIN"))
                .thenReturn(new CurrentUserContext(99L, UserRole.ADMIN, AuthStatus.APPROVED));

        mockMvc.perform(post("/admin/verification-reviews/1/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"REJECT\"}")
                        .header("Authorization", "Bearer ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void adminCanLoadReviewImage() throws Exception {
        when(jwtUtil.parse("ADMIN"))
                .thenReturn(new CurrentUserContext(99L, UserRole.ADMIN, AuthStatus.APPROVED));
        when(verificationReviewService.loadReviewImage(any(), eq(1L))).thenReturn(new StoredFileContent(
                new ByteArrayResource(new byte[]{4, 5, 6}), "image/jpeg", 3L));

        mockMvc.perform(get("/admin/verification-reviews/1/image")
                        .header("Authorization", "Bearer ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(new byte[]{4, 5, 6}));
    }

    @Test
    void adminHandleApprove_returnsOk() throws Exception {
        when(jwtUtil.parse("ADMIN"))
                .thenReturn(new CurrentUserContext(99L, UserRole.ADMIN, AuthStatus.APPROVED));

        mockMvc.perform(post("/admin/verification-reviews/1/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"APPROVE\"}")
                        .header("Authorization", "Bearer ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
