package com.campushub.dto.pickup;

import com.campushub.entity.enums.PickupCancelReason;
import com.campushub.entity.enums.PickupStatus;
import com.campushub.entity.enums.RewardType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代取服务详情，对应 {@code api_design.yaml} 的 {@code PickupRequestDetail}
 * （= PickupRequestSummary + 完整物品说明 + 接单方/接单时间/完成时间）。
 *
 * <p>契约以 allOf 继承 Summary，这里展平为独立字段以便 {@code @Builder} 组装；
 * 字段名与契约一一对应。取件凭证、完成凭证图片不在详情中返回（走受保护图片接口）。
 */
@Getter
@Builder
public class PickupRequestDetail {

    private final Long pickupId;
    private final String campus;
    private final String pickupLocation;
    private final String deliveryLocation;
    private final String itemDescriptionPreview;
    private final RewardType rewardType;
    private final BigDecimal rewardAmount;
    private final PickupStatus status;
    private final PickupCancelReason cancelReason;
    private final UserSummary publisher;
    private final LocalDateTime acceptDeadline;
    private final LocalDateTime createdAt;

    /** 完整物品说明。 */
    private final String itemDescription;
    /** 接单方公开摘要；未接单时为空。 */
    private final UserSummary acceptor;
    private final LocalDateTime acceptedAt;
    private final LocalDateTime completedAt;
}
