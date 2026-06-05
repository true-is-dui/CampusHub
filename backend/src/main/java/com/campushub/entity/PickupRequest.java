package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campushub.entity.enums.PickupCancelReason;
import com.campushub.entity.enums.PickupStatus;
import com.campushub.entity.enums.RewardType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代取请求实体，映射 pickup_requests 表，是代取业务的唯一主表。
 *
 * <p>统一维护代取业务状态流转：待支付 → 待接单 → 进行中 → 已完成 / 已取消。
 * 支付状态由 PaymentRecord 维护，业务状态仍以本表 status 为准。
 *
 * <p>本类是富领域模型，领域方法只维护对象自身状态和少量展示规则
 * （markAccepted、canViewPickupCredential 等），供 Service 层调用。
 * 权限校验、状态前置校验、跨模块协作由 PickupService 负责。
 */
@Data
@TableName("pickup_requests")
public class PickupRequest {

    /** 主键，数据库自增；对应 API pickupId */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布方用户 ID */
    private Long publisherId;

    /** 接单方用户 ID，未接单时为空 */
    private Long acceptorId;

    /** 校区，用于大厅筛选 */
    private String campus;

    /** 取件地点 */
    private String pickupLocation;

    /** 送达地点 */
    private String deliveryLocation;

    /** 物品说明 */
    private String itemDescription;

    /** 报酬类型 */
    private RewardType rewardType;

    /** 有报酬金额，范围 1-200；无报酬为空 */
    private BigDecimal rewardAmount;

    /** 取件凭证文件 ID，接单前不向非发布方展示 */
    private Long pickupCredentialFileId;

    /** 完成凭证文件 ID，接单方上传 */
    private Long completionProofFileId;

    /** 支付记录 ID，无报酬服务为空 */
    private Long paymentId;

    /** 代取请求状态 */
    private PickupStatus status;

    /** 取消原因，未取消时为空 */
    private PickupCancelReason cancelReason;

    /** 发布方主动取消时填写的补充说明 */
    private String cancelDetail;

    /** 接单截止时间 */
    private LocalDateTime acceptDeadline;

    /** 接单时间 */
    private LocalDateTime acceptedAt;

    /** 完成确认时间 */
    private LocalDateTime completedAt;

    /** 取消时间 */
    private LocalDateTime cancelledAt;

    /** 发布时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ---------------- 领域方法（仅供 Service 层内部调用） ----------------

    /** 有报酬服务发布后进入待支付，关联支付记录 */
    public void markWaitingPayment(Long paymentId) {
        this.paymentId = paymentId;
        this.status = PickupStatus.WAITING_PAYMENT;
    }

    /** 无报酬发布成功或有报酬支付成功后进入待接单 */
    public void markWaitingAccept() {
        this.status = PickupStatus.WAITING_ACCEPT;
    }

    /** 记录接单方并进入进行中 */
    public void markAccepted(Long acceptorId) {
        this.acceptorId = acceptorId;
        this.status = PickupStatus.IN_PROGRESS;
        this.acceptedAt = LocalDateTime.now();
    }

    /** 接单方上传单张完成凭证 */
    public void recordCompletionProof(Long fileId) {
        this.completionProofFileId = fileId;
    }

    /** 发布方确认完成后进入已完成 */
    public void markCompleted() {
        this.status = PickupStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /** 服务取消或超时后进入已取消 */
    public void markCancelled(PickupCancelReason reason, String detail) {
        this.status = PickupStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelDetail = detail;
        this.cancelledAt = LocalDateTime.now();
    }

    /** 仅接单方在接单成功后可查看取件凭证 */
    public boolean canViewPickupCredential(Long userId) {
        return isAcceptor(userId) && this.acceptorId != null;
    }

    /** 仅待接单状态可进入代取需求大厅 */
    public boolean isHallVisible() {
        return this.status == PickupStatus.WAITING_ACCEPT;
    }

    /** 判断用户是否为发布方 */
    public boolean isPublisher(Long userId) {
        return userId != null && userId.equals(this.publisherId);
    }

    /** 判断用户是否为接单方 */
    public boolean isAcceptor(Long userId) {
        return userId != null && userId.equals(this.acceptorId);
    }

    /** 是否为有报酬服务 */
    public boolean isPaid() {
        return this.rewardType == RewardType.PAID;
    }
}
