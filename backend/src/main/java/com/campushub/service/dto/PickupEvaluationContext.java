package com.campushub.service.dto;

import com.campushub.entity.enums.PickupStatus;
import lombok.Builder;
import lombok.Getter;

/**
 * 代取服务评价上下文，由 {@code PickupService.queryPickupEvaluationContext} 返回给评价模块，
 * 对应 {@code class_design.md} 中 {@code PickupService} 向评价模块提供「发布方、接单方和服务状态」。
 *
 * <p>跨模块只传业务层 DTO、不传 {@code PickupRequest} 实体（第四批准则）。评价模块据此判断
 * 参与者、推导被评价人与角色、校验服务是否已完成，不直接访问代取表。
 */
@Getter
@Builder
public class PickupEvaluationContext {

    private final Long pickupId;
    private final Long publisherId;
    /** 接单方 ID；未接单时为 null。 */
    private final Long acceptorId;
    private final PickupStatus status;
}
