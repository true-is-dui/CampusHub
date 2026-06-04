package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import com.campushub.config.AuthInterceptor;
import com.campushub.config.CurrentUserContext;
import com.campushub.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getNotifications(
            HttpServletRequest request,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        return ApiResponse.ok(notificationService.queryMyNotices(ctx.getCurrentUserId(), page, pageSize));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Object>> getUnreadCount(HttpServletRequest request) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        long count = notificationService.countUnread(ctx.getCurrentUserId());
        return ApiResponse.ok(Map.of("unreadCount", count));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            HttpServletRequest request,
            @PathVariable Long notificationId
    ) {
        CurrentUserContext ctx = (CurrentUserContext) request.getAttribute(AuthInterceptor.CONTEXT_KEY);
        notificationService.markRead(notificationId, ctx.getCurrentUserId());
        return ApiResponse.ok();
    }
}
