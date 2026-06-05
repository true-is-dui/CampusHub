package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campushub.entity.enums.ReviewStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实名认证审核记录实体，映射 verification_reviews 表。
 *
 * <p>保留多条历史审核记录，用于追溯每次认证提交和处理结果。
 * submittedStudentId、submittedRealName 只是当次申请提交的快照，
 * 仅用于审核和历史追溯；审核通过后由 Service 将通过值同步到 users 表，
 * 历史快照不参与后续业务身份判断。
 *
 * <p>同一用户同一时间只能有一条 PENDING 记录，由 Service 层在提交时校验。
 */
@Data
@TableName("verification_reviews")
public class VerificationReview {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请认证的用户 ID */
    private Long userId;

    /** 认证材料文件 ID，关联 stored_files.id */
    private Long materialFileId;

    /** 当次申请提交的学号快照 */
    private String submittedStudentId;

    /** 当次申请提交的真实姓名快照 */
    private String submittedRealName;

    /** 审核状态 */
    private ReviewStatus status;

    /** 审核管理员 ID，未处理时为空 */
    private Long reviewerId;

    /** 驳回原因 */
    private String rejectReason;

    /** 提交审核时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime submittedAt;

    /** 审核处理时间，未处理时为空 */
    private LocalDateTime reviewedAt;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ---------------- 领域方法（仅供 Service 层内部调用） ----------------

    /** 记录审核通过结果 */
    public void markApproved(Long reviewerId) {
        this.status = ReviewStatus.APPROVED;
        this.reviewerId = reviewerId;
        this.reviewedAt = LocalDateTime.now();
    }

    /** 记录审核驳回结果 */
    public void markRejected(Long reviewerId, String reason) {
        this.status = ReviewStatus.REJECTED;
        this.reviewerId = reviewerId;
        this.rejectReason = reason;
        this.reviewedAt = LocalDateTime.now();
    }

    /** 是否仍为待审核状态 */
    public boolean isPending() {
        return this.status == ReviewStatus.PENDING;
    }
}
