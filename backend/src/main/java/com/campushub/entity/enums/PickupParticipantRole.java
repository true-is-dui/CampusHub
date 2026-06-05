package com.campushub.entity.enums;

/**
 * 被评价者在代取服务中的角色，对应 evaluations.reviewee_role。
 * 用于分别统计用户作为发布方、作为接单方收到的评价表现。
 */
public enum PickupParticipantRole {
    /** 被评价者在该代取服务中是发布方 */
    PUBLISHER,
    /** 被评价者在该代取服务中是接单方 */
    ACCEPTOR
}
