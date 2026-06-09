package com.campushub.dto.point;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 积分余额响应，对应 {@code api_design.yaml} {@code GET /users/me/point-balance} 的 data
 * （{@code { pointBalance }}）。
 */
@Getter
@RequiredArgsConstructor
public class PointBalanceResponse {

    /** 当前用户的平台积分余额（>=0）。 */
    private final Long pointBalance;
}
