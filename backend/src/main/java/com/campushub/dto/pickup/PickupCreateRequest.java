package com.campushub.dto.pickup;

import com.campushub.entity.enums.RewardType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发布代取服务的 multipart 表单文本字段，对应 {@code api_design.yaml} 的
 * {@code PickupCreateRequest}（取件凭证图片 {@code pickupCredential} 不在此 DTO，
 * 由 Controller 以独立 {@code @RequestPart} 接收）。
 *
 * <p>格式校验全部声明式（第四批准则：格式归 DTO）：必填、长度、校区枚举、金额范围、
 * 接单截止时间须晚于当前。跨字段约束「PAID 必填金额且 UNPAID 必须为空」用
 * {@link AssertTrue} 表达，DTO 内即可判定，无需下沉 Service。
 *
 * <p>用 {@code @Data} 生成 getter/setter，供 Spring MVC 按 multipart 字段名绑定。
 */
@Data
public class PickupCreateRequest {

    /** 校区，用于大厅筛选。取值对齐契约内联枚举。 */
    @NotBlank(message = "校区不能为空")
    @Pattern(regexp = "GULOU|XIANLIN|PUKOU|SUZHOU",
            message = "校区只能为 GULOU、XIANLIN、PUKOU 或 SUZHOU")
    private String campus;

    /** 取件地点。 */
    @NotBlank(message = "取件地点不能为空")
    @Size(max = 100, message = "取件地点不能超过 100 字")
    private String pickupLocation;

    /** 送达地点。 */
    @NotBlank(message = "送达地点不能为空")
    @Size(max = 100, message = "送达地点不能超过 100 字")
    private String deliveryLocation;

    /** 物品说明。 */
    @NotBlank(message = "物品说明不能为空")
    @Size(max = 500, message = "物品说明不能超过 500 字")
    private String itemDescription;

    /** 报酬类型；非法值由 Jackson/绑定阶段拒绝。 */
    @NotNull(message = "报酬类型不能为空")
    private RewardType rewardType;

    /** 有报酬金额，范围 1-200；无报酬必须为空（见 {@link #isRewardAmountConsistent()}）。 */
    @DecimalMin(value = "1", message = "报酬金额不能低于 1 元")
    @DecimalMax(value = "200", message = "报酬金额不能高于 200 元")
    private BigDecimal rewardAmount;

    /** 接单截止时间，必须晚于当前时间。 */
    @NotNull(message = "接单截止时间不能为空")
    @Future(message = "接单截止时间必须晚于当前时间")
    private LocalDateTime acceptDeadline;

    /**
     * 跨字段约束：PAID 时金额必填，UNPAID 时金额必须为空。
     * 校验失败时 errors 以字段名 {@code rewardAmount} 为 key（契约参数校验用字段名作 key）。
     */
    @AssertTrue(message = "有报酬服务必须填写金额，无报酬服务不能填写金额")
    public boolean isRewardAmountConsistent() {
        if (rewardType == null) {
            return true; // 交给 @NotNull 报错，避免重复提示
        }
        return rewardType == RewardType.PAID
                ? rewardAmount != null
                : rewardAmount == null;
    }
}
