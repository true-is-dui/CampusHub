package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campushub.entity.enums.PointTransactionType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分流水实体，映射 point_transactions 表。
 *
 * <p>记录用户每一笔积分变动（认证赠送、签到、发布扣减、取消退回、完成入账），冗余记录变动后
 * 余额 {@code balanceAfter} 便于直接展示历史，不需回放计算。积分为平台内虚拟资产，不可充值提现，
 * 故无第三方交易号、商户订单号等支付字段。
 *
 * <p>{@code relatedPickupId} 仅用于代取相关流水的溯源（非代取流水为空），不建立业务外键约束。
 * 用户当前余额存于 {@code users.point_balance}，由 {@code PointService} 在每次写流水时同一事务内同步。
 */
@Data
@TableName("point_transactions")
public class PointTransaction {

    /** 主键，数据库自增；对应 API transactionId */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流水所属用户 ID */
    private Long userId;

    /** 流水类型 */
    private PointTransactionType type;

    /** 积分变动量，正数入账，负数出账（发布扣减） */
    private Long amount;

    /** 本次变动后的积分余额，冗余记录便于直接展示 */
    private Long balanceAfter;

    /** 关联代取请求 ID；非代取相关流水为空；仅用于溯源 */
    private Long relatedPickupId;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
