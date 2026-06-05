package com.campushub.entity.enums;

/**
 * 用户实名认证状态，对应 users.auth_status。
 * 仅认证通过（APPROVED）的用户才能发布、接单、评价。
 */
public enum AuthStatus {
    /** 未认证：注册后的初始状态 */
    UNVERIFIED,
    /** 审核中：已提交实名材料，等待管理员审核 */
    REVIEWING,
    /** 已通过：可参与代取业务 */
    APPROVED,
    /** 已驳回：审核未通过，可重新提交 */
    REJECTED
}
