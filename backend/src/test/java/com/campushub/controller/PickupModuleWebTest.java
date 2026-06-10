package com.campushub.controller;

import com.campushub.common.CurrentUserContext;
import com.campushub.common.PageResult;
import com.campushub.dto.evaluation.PickupEvaluationItem;
import com.campushub.dto.evaluation.ReceivedEvaluationDetail;
import com.campushub.dto.pickup.PickupCancelResult;
import com.campushub.dto.pickup.PickupRequestSummary;
import com.campushub.dto.pickup.UserSummary;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.PickupCancelReason;
import com.campushub.entity.enums.PickupParticipantRole;
import com.campushub.entity.enums.PickupStatus;
import com.campushub.entity.enums.RatingLevel;
import com.campushub.entity.enums.UserRole;
import com.campushub.security.JwtUtil;
import com.campushub.service.PickupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 代取模块 Web 层切片测试：验证路由、公开 / 受保护接口的鉴权拦截、参数校验与统一响应包装。
 * PickupService 与 JwtUtil 以 {@link MockitoBean} 替换，不连数据库。
 */
@WebMvcTest(controllers = PickupController.class)
class PickupModuleWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PickupService pickupService;
    @MockitoBean
    private com.campushub.service.EvaluationService evaluationService;
    @MockitoBean
    private JwtUtil jwtUtil;

    @BeforeEach
    void stubToken() {
        when(jwtUtil.parse("VALID"))
                .thenReturn(new CurrentUserContext(7L, UserRole.USER, AuthStatus.APPROVED));
    }

    @Test
    void hall_isPublic_returnsOk_withoutToken() throws Exception {
        when(pickupService.queryHall(any(), any(), any()))
                .thenReturn(PageResult.of(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20),
                        List.<PickupRequestSummary>of()));

        mockMvc.perform(get("/pickup-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    void detail_isPublic_returnsOk_withoutToken() throws Exception {
        when(pickupService.queryDetail(5L)).thenReturn(null);

        mockMvc.perform(get("/pickup-requests/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void accept_requiresAuth_401WithoutToken() throws Exception {
        mockMvc.perform(post("/pickup-requests/5/accept"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void credential_requiresAuth_401WithoutToken() throws Exception {
        // 受保护图片接口：路径多一段，不在可选鉴权放行范围内
        mockMvc.perform(get("/pickup-requests/5/credential"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void cancel_returnsCancelled_whenAuthenticated() throws Exception {
        when(pickupService.cancelPickup(5L, 7L, "改主意了"))
                .thenReturn(PickupCancelResult.builder()
                        .status(PickupStatus.CANCELLED)
                        .cancelReason(PickupCancelReason.USER_CANCELLED)
                        .build());

        mockMvc.perform(post("/pickup-requests/5/cancel")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"改主意了\"}")
                        .header("Authorization", "Bearer VALID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelReason").value("USER_CANCELLED"));
    }

    // ---------------- 评价接口 ----------------

    @Test
    void submitEvaluation_requiresAuth_401WithoutToken() throws Exception {
        // 受保护写接口：路径两段，不在可选鉴权放行范围内
        mockMvc.perform(post("/pickup-requests/5/evaluations")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"ratingLevel\":\"GOOD\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void submitEvaluation_badWithoutContent_returns400() throws Exception {
        // BAD 缺 content 由 DTO @AssertTrue 拦截，参数校验 400，不进 service
        mockMvc.perform(post("/pickup-requests/5/evaluations")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"ratingLevel\":\"BAD\"}")
                        .header("Authorization", "Bearer VALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void evaluationEligibility_requiresAuth_401WithoutToken() throws Exception {
        mockMvc.perform(get("/pickup-requests/5/evaluation-eligibility"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void pickupEvaluations_requiresAuth_401WithoutToken() throws Exception {
        mockMvc.perform(get("/pickup-requests/5/evaluations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void pickupEvaluations_returnsList_whenAuthenticated() throws Exception {
        when(evaluationService.queryPickupEvaluations(5L, 7L))
                .thenReturn(List.of(PickupEvaluationItem.builder()
                        .evaluationId(12L)
                        .reviewer(UserSummary.builder().userId(7L).nickname("发布侠").build())
                        .reviewerRoleInBusiness(PickupParticipantRole.PUBLISHER)
                        .reviewee(UserSummary.builder().userId(8L).nickname("接单侠").build())
                        .revieweeRoleInBusiness(PickupParticipantRole.ACCEPTOR)
                        .ratingLevel(RatingLevel.GOOD)
                        .content("很快")
                        .build()));

        mockMvc.perform(get("/pickup-requests/5/evaluations")
                        .header("Authorization", "Bearer VALID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].evaluationId").value(12))
                .andExpect(jsonPath("$.data[0].reviewer.nickname").value("发布侠"))
                .andExpect(jsonPath("$.data[0].reviewee.nickname").value("接单侠"))
                .andExpect(jsonPath("$.data[0].ratingLevel").value("GOOD"));
    }

    @Test
    void receivedEvaluation_returnsCurrentUsersReceivedEvaluation() throws Exception {
        when(evaluationService.queryReceivedEvaluation(5L, 7L))
                .thenReturn(ReceivedEvaluationDetail.builder()
                        .evaluationId(12L)
                        .reviewer(UserSummary.builder().userId(8L).nickname("接单侠").build())
                        .revieweeRoleInBusiness(PickupParticipantRole.PUBLISHER)
                        .ratingLevel(RatingLevel.GOOD)
                        .content("沟通很清楚")
                        .build());

        mockMvc.perform(get("/pickup-requests/5/received-evaluation")
                        .header("Authorization", "Bearer VALID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.evaluationId").value(12))
                .andExpect(jsonPath("$.data.reviewer.userId").value(8))
                .andExpect(jsonPath("$.data.ratingLevel").value("GOOD"))
                .andExpect(jsonPath("$.data.content").value("沟通很清楚"));
    }
}
