package com.campushub.controller;

import com.campushub.common.CurrentUserContext;
import com.campushub.common.PageResult;
import com.campushub.dto.pickup.PickupCancelResult;
import com.campushub.dto.pickup.PickupRequestSummary;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.PickupCancelReason;
import com.campushub.entity.enums.PickupStatus;
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
}
