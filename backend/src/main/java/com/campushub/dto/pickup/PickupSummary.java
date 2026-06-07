package com.campushub.dto.pickup;

import com.campushub.entity.enums.PickupCancelReason;
import com.campushub.entity.enums.PickupStatus;
import com.campushub.entity.enums.RewardType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代取服务通用展示摘要，对应 {@code api_design.yaml} 的 {@code PickupSummary}，
 * 用于「我的发布」「我的接单」列表。与大厅项 {@link PickupRequestSummary} 的差异：
 * 不含 publisher / acceptDeadline，含 completedAt。
 */
@Getter
@Builder
public class PickupSummary {

    private final Long pickupId;
    private final String campus;
    private final String pickupLocation;
    private final String deliveryLocation;
    private final String itemDescriptionPreview;
    private final RewardType rewardType;
    private final BigDecimal rewardAmount;
    private final PickupStatus status;
    /** 仅 status=CANCELLED 时非空。 */
    private final PickupCancelReason cancelReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime completedAt;
}
