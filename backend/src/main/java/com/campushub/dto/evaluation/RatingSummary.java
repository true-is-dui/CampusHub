package com.campushub.dto.evaluation;

import lombok.Builder;
import lombok.Getter;

/**
 * 用户好评率摘要，对应 {@code api_design.yaml} 的 {@code RatingSummary}。
 *
 * <p>按被评价人在代取服务中的角色分别统计：{@code publisherRoleSummary} 为该用户作为发布方
 * 收到的评价表现，{@code acceptorRoleSummary} 为其作为接单方收到的评价表现。两者均动态聚合、
 * 不缓存；用户在某一角色下无任何评价时，对应统计各计数为 0、{@code positiveRate} 为 0.0。
 */
@Getter
@Builder
public class RatingSummary {

    private final Long userId;
    private final RatingRoleSummary publisherRoleSummary;
    private final RatingRoleSummary acceptorRoleSummary;
}
