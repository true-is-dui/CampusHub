package com.campushub.controller;

import com.campushub.common.CurrentUserContext;
import com.campushub.common.PageResult;
import com.campushub.dto.pickup.PickupSummary;
import com.campushub.dto.user.LoginSession;
import com.campushub.dto.user.UserMeResponse;
import com.campushub.dto.user.UserPublicProfile;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.PickupParticipantRole;
import com.campushub.entity.enums.PickupStatus;
import com.campushub.entity.enums.UserRole;
import com.campushub.security.AuthInterceptor;
import com.campushub.security.JwtUtil;
import com.campushub.service.AuthService;
import com.campushub.service.PickupService;
import com.campushub.service.UserService;
import com.campushub.service.VerificationReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户模块 Web 层切片测试：验证路由、参数校验、统一响应包装、鉴权拦截器与
 * 全局异常处理是否正确串联。Service 与 JWT 工具以 {@link MockitoBean} 替换，不连数据库。
 *
 * <p>{@code @WebMvcTest} 会装配 Controller、{@code WebMvcConfig}（含拦截器与
 * {@code @CurrentUser} 解析器）和 {@code GlobalExceptionHandler}，但不加载 Service、
 * Mapper 等非 Web 组件，故对它们用 MockitoBean 显式提供。
 */
@WebMvcTest(controllers = {AuthController.class, UserController.class})
class UserModuleWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private VerificationReviewService verificationReviewService;
    @MockitoBean
    private PickupService pickupService;
    @MockitoBean
    private com.campushub.service.NotificationService notificationService;
    @MockitoBean
    private com.campushub.service.EvaluationService evaluationService;
    @MockitoBean
    private com.campushub.service.PointService pointService;
    // 拦截器依赖 JwtUtil；Web 切片不加载它，需 MockitoBean 注入
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void register_returnsOk_andDelegatesToService() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"Passw0rd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void register_rejectsInvalidPassword_with40001() throws Exception {
        // 密码过短且不含数字，违反 @Size/@Pattern → @Valid 拦截 → 40001
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void login_returnsToken() throws Exception {
        when(authService.login(anyString(), anyString())).thenReturn(new LoginSession("JWT"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"Passw0rd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value("JWT"));
    }

    @Test
    void me_returnsProfile_whenTokenValid() throws Exception {
        // 拦截器用 JwtUtil.parse 还原身份；mock 成返回固定上下文
        when(jwtUtil.parse("VALID"))
                .thenReturn(new CurrentUserContext(7L, UserRole.USER, AuthStatus.APPROVED));
        when(userService.getCurrentUser(7L)).thenReturn(UserMeResponse.builder()
                .userId(7L)
                .username("alice")
                .nickname("小爱")
                .studentIdMasked("20******34")
                .authStatus(AuthStatus.APPROVED)
                .role(UserRole.USER)
                .build());

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer VALID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(7))
                .andExpect(jsonPath("$.data.studentIdMasked").value("20******34"));
    }

    @Test
    void me_returns401_whenTokenMissing() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void myPickups_missingRole_400() throws Exception {
        // role 为必填查询参数，缺失 → 400（路由挂在 UserController，委托 PickupService）
        when(jwtUtil.parse("VALID"))
                .thenReturn(new CurrentUserContext(7L, UserRole.USER, AuthStatus.APPROVED));
        mockMvc.perform(get("/users/me/pickup-requests").header("Authorization", "Bearer VALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void myPickups_returnsList_whenRoleProvided() throws Exception {
        when(jwtUtil.parse("VALID"))
                .thenReturn(new CurrentUserContext(7L, UserRole.USER, AuthStatus.APPROVED));
        when(pickupService.queryMyPublished(any(), any(), any())).thenReturn(
                PageResult.of(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<PickupSummary>(1, 20, 0),
                        java.util.List.of()));

        mockMvc.perform(get("/users/me/pickup-requests")
                        .param("role", PickupParticipantRole.PUBLISHER.name())
                        .param("status", PickupStatus.WAITING_ACCEPT.name())
                        .header("Authorization", "Bearer VALID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    // ---------------- 用户公开主页 / 评价（公开访问，白名单放行） ----------------

    @Test
    void publicProfile_isPublic_returnsOk_withoutToken() throws Exception {
        when(userService.getPublicProfile(5L)).thenReturn(
                UserPublicProfile.builder().nickname("小明").college("软件学院").build());

        mockMvc.perform(get("/users/5/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("小明"))
                // 默认 includeRating=false，评价摘要为 null
                .andExpect(jsonPath("$.data.ratingSummary").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void ratingSummary_isPublic_returnsOk_withoutToken() throws Exception {
        when(evaluationService.queryUserRatingSummary(5L)).thenReturn(
                com.campushub.dto.evaluation.RatingSummary.builder().userId(5L).build());

        mockMvc.perform(get("/users/5/rating-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(5));
    }
}
