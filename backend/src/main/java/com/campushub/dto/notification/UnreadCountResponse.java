package com.campushub.dto.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 未读通知数量响应，对应 {@code api_design.yaml} 中 {@code GET /users/me/notifications/unread-count}
 * 的 {@code { unreadCount }} 结构。
 */
@Getter
@RequiredArgsConstructor
public class UnreadCountResponse {

    /** 未读通知数量（≥0）。 */
    private final Long unreadCount;
}
