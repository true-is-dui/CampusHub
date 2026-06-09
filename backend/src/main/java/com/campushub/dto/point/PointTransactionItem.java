package com.campushub.dto.point;

import com.campushub.entity.PointTransaction;
import com.campushub.entity.enums.PointTransactionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 积分流水列表项，对应 {@code api_design.yaml} 的 {@code PointTransactionItem}。
 *
 * <p>不直接暴露实体；{@code type} 用 {@link PointTransactionType} 枚举（契约定义为 enum）。
 * {@code relatedPickupId} 对非代取流水（赠送/签到）为 null。
 */
@Getter
@Builder
public class PointTransactionItem {

    private final Long transactionId;
    private final PointTransactionType type;
    /** 本次积分变动量，正数入账、负数出账（发布扣减）。 */
    private final Long amount;
    /** 本次变动后的积分余额（>=0）。 */
    private final Long balanceAfter;
    /** 关联代取服务 ID；非代取相关的赠送/签到流水为空。 */
    private final Long relatedPickupId;
    private final LocalDateTime createdAt;

    /** 实体 → VO。 */
    public static PointTransactionItem from(PointTransaction tx) {
        return PointTransactionItem.builder()
                .transactionId(tx.getId())
                .type(tx.getType())
                .amount(tx.getAmount())
                .balanceAfter(tx.getBalanceAfter())
                .relatedPickupId(tx.getRelatedPickupId())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
