package com.campushub.dto.evaluation;

import com.campushub.entity.enums.PickupParticipantRole;
import lombok.Builder;
import lombok.Getter;

/**
 * 用户以某一业务角色（发布方 / 接单方）收到的评价统计，对应 {@code api_design.yaml} 的
 * {@code RatingRoleSummary}。
 *
 * <p>动态聚合计算、不缓存（DB 设计 §5.1：好评率统一从 evaluations 实时聚合）。
 * {@code positiveRate} = 好评数 / 评价总数；总数为 0 时由聚合方返回 0.0。
 */
@Getter
@Builder
public class RatingRoleSummary {

    private final PickupParticipantRole revieweeRoleInBusiness;
    private final Integer positiveCount;
    private final Integer neutralCount;
    private final Integer negativeCount;
    private final Integer totalCount;
    private final Double positiveRate;
}
