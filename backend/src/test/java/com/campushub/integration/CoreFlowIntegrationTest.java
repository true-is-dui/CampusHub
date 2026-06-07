package com.campushub.integration;

import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.dto.pickup.CompletionConfirmResult;
import com.campushub.dto.pickup.PickupAcceptResult;
import com.campushub.dto.pickup.PickupCreateResult;
import com.campushub.dto.user.LoginSession;
import com.campushub.dto.user.UserMeResponse;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.PaymentStatus;
import com.campushub.entity.enums.PickupStatus;
import com.campushub.entity.enums.UserRole;
import com.campushub.security.JwtUtil;
import com.campushub.service.AuthService;
import com.campushub.service.PickupService;
import com.campushub.service.UserService;
import com.campushub.service.VerificationReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 面向 P4 核心链路的 HTTP 集成测试：
 * 使用完整 Spring Boot Web 上下文、真实 JWT 解析链路和 MockMvc，
 * 通过替换 service bean 避免数据库依赖，重点验证路由、鉴权、参数校验、
 * 统一响应和跨控制器主流程。
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
@AutoConfigureMockMvc
class CoreFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private VerificationReviewService verificationReviewService;
    @MockitoBean
    private PickupService pickupService;

    @Test
    void normalFlow_registerLoginPublishAcceptUploadProofAndComplete() throws Exception {
        String publisherToken = jwtUtil.generateToken(7L, UserRole.USER, AuthStatus.APPROVED);
        String acceptorToken = jwtUtil.generateToken(8L, UserRole.USER, AuthStatus.APPROVED);

        when(authService.login("publisher", "Passw0rd")).thenReturn(new LoginSession(publisherToken));
        when(authService.login("runner", "Passw0rd")).thenReturn(new LoginSession(acceptorToken));
        when(userService.getCurrentUser(7L)).thenReturn(UserMeResponse.builder()
                .userId(7L)
                .username("publisher")
                .nickname("发布方")
                .authStatus(AuthStatus.APPROVED)
                .role(UserRole.USER)
                .build());
        when(pickupService.publishPickup(eq(7L), any(), any())).thenReturn(PickupCreateResult.builder()
                .pickupId(101L)
                .status(PickupStatus.WAITING_ACCEPT)
                .build());
        when(pickupService.acceptPickup(101L, 8L)).thenReturn(PickupAcceptResult.builder()
                .status(PickupStatus.IN_PROGRESS)
                .acceptedAt(LocalDateTime.now())
                .build());
        when(pickupService.confirmComplete(101L, 7L)).thenReturn(CompletionConfirmResult.builder()
                .status(PickupStatus.COMPLETED)
                .paymentStatus(PaymentStatus.SETTLED)
                .completedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"publisher\",\"password\":\"Passw0rd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"publisher\",\"password\":\"Passw0rd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value(publisherToken));

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + publisherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("publisher"));

        mockMvc.perform(multipart("/pickup-requests")
                        .file("pickupCredential", new byte[]{1, 2, 3})
                        .param("campus", "XIANLIN")
                        .param("pickupLocation", "菜鸟驿站")
                        .param("deliveryLocation", "教学楼A")
                        .param("itemDescription", "书籍")
                        .param("rewardType", "UNPAID")
                        .param("acceptDeadline", LocalDateTime.now().plusHours(2).toString())
                        .header("Authorization", "Bearer " + publisherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pickupId").value(101))
                .andExpect(jsonPath("$.data.status").value("WAITING_ACCEPT"));

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"runner\",\"password\":\"Passw0rd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value(acceptorToken));

        mockMvc.perform(post("/pickup-requests/101/accept")
                        .header("Authorization", "Bearer " + acceptorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(multipart("/pickup-requests/101/completion-proof")
                        .file("proofImage", new byte[]{9, 9, 9})
                        .header("Authorization", "Bearer " + acceptorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/pickup-requests/101/completion-confirmation")
                        .header("Authorization", "Bearer " + publisherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("SETTLED"));
    }

    @Test
    void abnormalFlow_publishWithoutLogin_returns401() throws Exception {
        mockMvc.perform(multipart("/pickup-requests")
                        .file("pickupCredential", new byte[]{1})
                        .param("campus", "XIANLIN")
                        .param("pickupLocation", "A")
                        .param("deliveryLocation", "B")
                        .param("itemDescription", "书籍")
                        .param("rewardType", "UNPAID")
                        .param("acceptDeadline", LocalDateTime.now().plusHours(1).toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void abnormalFlow_publishWithInvalidRewardPayload_returns40001() throws Exception {
        String publisherToken = jwtUtil.generateToken(7L, UserRole.USER, AuthStatus.APPROVED);

        mockMvc.perform(multipart("/pickup-requests")
                        .file("pickupCredential", new byte[]{1})
                        .param("campus", "XIANLIN")
                        .param("pickupLocation", "A")
                        .param("deliveryLocation", "B")
                        .param("itemDescription", "书籍")
                        .param("rewardType", "PAID")
                        .param("acceptDeadline", LocalDateTime.now().plusHours(1).toString())
                        .header("Authorization", "Bearer " + publisherToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.errors.rewardAmount").exists());
    }

    @Test
    void abnormalFlow_duplicateAccept_returns40901() throws Exception {
        String acceptorToken = jwtUtil.generateToken(8L, UserRole.USER, AuthStatus.APPROVED);
        doThrow(new BusinessException(ErrorCode.CONFLICT, "该需求已被其他用户接单"))
                .when(pickupService).acceptPickup(101L, 8L);

        mockMvc.perform(post("/pickup-requests/101/accept")
                        .header("Authorization", "Bearer " + acceptorToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("该需求已被其他用户接单"));
    }
}
