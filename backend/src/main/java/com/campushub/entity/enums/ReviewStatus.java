package com.campushub.entity.enums;

/**
 * 实名认证审核状态，对应 verification_reviews.status。
 */
public enum ReviewStatus {
    /** 待审核：用户已提交，等待管理员处理 */
    PENDING,
    /** 审核通过 */
    APPROVED,
    /** 审核驳回 */
    REJECTED
}
