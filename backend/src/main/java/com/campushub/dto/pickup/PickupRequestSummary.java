package com.campushub.dto.pickup;

import com.campushub.entity.enums.PickupCancelReason;
import com.campushub.entity.enums.PickupStatus;
import com.campushub.entity.enums.RewardType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代取大厅公开列表项，对应 {@code api_design.yaml} 的 {@code PickupRequestSummary}。
 * 物品说明为预览（preview），不含取件/完成凭证等敏感字段。
 */
@Getter
@Builder
public class PickupRequestSummary {

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
    private final UserSummary publisher;
    private final LocalDateTime acceptDeadline;
    private final LocalDateTime createdAt;
}
