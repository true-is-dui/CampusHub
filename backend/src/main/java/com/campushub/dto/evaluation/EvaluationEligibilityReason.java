package com.campushub.dto.evaluation;

/**
 * 评价不可用原因，对应 {@code api_design.yaml} 中 {@code EvaluationEligibility.reason} 的内联枚举。
 *
 * <p>纯对外 VO 枚举（无数据库列映射），按第六批准则归 {@code dto} 包，不入 {@code entity.enums}。
 * 仅用于评价资格查询时向前端解释「为何不能评价」，决定是否展示评价入口。
 */
public enum EvaluationEligibilityReason {

    /** 代取服务尚未完成（仅 COMPLETED 可评价）。 */
    NOT_COMPLETED,

    /** 当前用户不是该代取服务的参与者（既非发布方也非接单方）。 */
    NOT_PARTICIPANT,

    /** 当前用户已对该代取服务评价过。 */
    ALREADY_EVALUATED,

    /** 账号状态不允许（如未实名认证）。 */
    ACCOUNT_NOT_ALLOWED
}
