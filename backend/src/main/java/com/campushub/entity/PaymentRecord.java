package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campushub.entity.enums.BusinessType;
import com.campushub.entity.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体，映射 payment_records 表。
 *
 * <p>只维护平台内部支付记录和支付状态，不理解具体代取业务对象，
 * 也不反向改写代取请求状态。businessType、businessTraceNo 仅用于
 * 幂等、日志追踪、回调定位和异常排查，不作为理解业务的依据。
 * 代取请求到支付记录的关联由 PickupRequest.paymentId 维护。
 *
 * <p>status 只保留五类稳定状态；支付失败、回调失败等异常通过
 * failureReason 或应用日志记录，不写入 status。
 */
@Data
@TableName("payment_records")
public class PaymentRecord {

    /** 主键，数据库自增；对应 API paymentId */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 付款方用户 ID，即代取发布方 */
    private Long payerId;

    /** 收款方用户 ID，即接单方；结算前可为空 */
    private Long receiverId;

    /** 业务类型，当前仅 PICKUP_REQUEST */
    private BusinessType businessType;

    /** 业务追踪号，用于幂等、日志、回调定位；非业务外键 */
    private String businessTraceNo;

    /** 支付金额，单位元 */
    private BigDecimal amount;

    /** 商户订单号，必须唯一 */
    private String outTradeNo;

    /** 第三方交易号，支付宝沙箱回调返回 */
    private String tradeNo;

    /** 支付状态 */
    private PaymentStatus status;

    /** 支付入口/支付链接，由后端保存并通过接口返回 */
    private String payEntry;

    /** 支付过期时间，当前 MVP 为创建后 3 分钟 */
    private LocalDateTime expireAt;

    /** 支付成功时间 */
    private LocalDateTime paidAt;

    /** 退款成功时间 */
    private LocalDateTime refundedAt;

    /** 结算成功时间 */
    private LocalDateTime settledAt;

    /** 待支付关闭时间 */
    private LocalDateTime closedAt;

    /** 关闭原因，例如发布方取消或支付超时 */
    private String closeReason;

    /** 第三方支付失败或内部处理失败说明 */
    private String failureReason;

    /** 最近一次状态变化时间 */
    private LocalDateTime statusChangedAt;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ---------------- 领域方法（仅供 Service 层内部调用） ----------------

    /** 标记预付款已支付 */
    public void markPaid(String tradeNo) {
        this.status = PaymentStatus.PAID;
        this.tradeNo = tradeNo;
        this.paidAt = LocalDateTime.now();
        this.statusChangedAt = this.paidAt;
    }

    /** 关闭待支付记录；覆盖发布方取消和支付超时两种来源 */
    public void markClosed(String reason) {
        this.status = PaymentStatus.CLOSED;
        this.closeReason = reason;
        this.closedAt = LocalDateTime.now();
        this.statusChangedAt = this.closedAt;
    }

    /** 已支付服务取消时标记退款 */
    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
        this.statusChangedAt = this.refundedAt;
    }

    /** 发布方确认完成后标记结算 */
    public void markSettled(Long receiverId) {
        this.status = PaymentStatus.SETTLED;
        this.receiverId = receiverId;
        this.settledAt = LocalDateTime.now();
        this.statusChangedAt = this.settledAt;
    }

    /** 是否处于待支付状态 */
    public boolean isWaitingPay() {
        return this.status == PaymentStatus.WAITING_PAY;
    }
}
