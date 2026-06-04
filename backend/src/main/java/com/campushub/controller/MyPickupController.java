package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import com.campushub.config.AuthInterceptor;
import com.campushub.config.CurrentUserContext;
import com.campushub.service.PickupService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users/me/pickup-requests")
@RequiredArgsConstructor
public class MyPickupController {

    private final PickupService pickupService;

    @GetMapping
    public ApiResponse<Map<String, Object>> queryMyPickups(
            HttpServletRequest request,
            @RequestParam("role") String role,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        if ("PUBLISHER".equalsIgnoreCase(role)) {
            return ApiResponse.ok(pickupService.queryMyPublished(ctx.getCurrentUserId(), status, page, pageSize));
        } else if ("ACCEPTOR".equalsIgnoreCase(role)) {
            return ApiResponse.ok(pickupService.queryMyAccepted(ctx.getCurrentUserId(), status, page, pageSize));
        } else {
            throw new BusinessException(40001, "role 必填且只能为 PUBLISHER 或 ACCEPTOR");
        }
    }
}
