package com.campushub.common;

import lombok.Getter;

/**
 * 错误响应 {@code errors.reason} 的语义码，集中管理，避免散落成裸字符串导致拼写漂移。
 *
 * <p>取值对齐 {@code api_design.yaml} 中各错误响应示例的 {@code reason}。枚举名即写入
 * {@code errors.reason} 的值（{@link #name()}），{@code defaultMessage} 为缺省中文提示。
 *
 * <p>注意契约的收敛粒度：注册重复、重复提交认证、学号已被占用等多种 409 业务冲突，
 * 契约统一归到 {@link #DUPLICATE_OR_CONFLICTED_OPERATION} 一个码，不细分。
 *
 * <p>{@link #INVALID_CREDENTIALS} 是已知的契约缺口：登录凭证错误（用户不存在/密码错）
 * 的 reason 在 {@code api_design.yaml} 中尚未定义，暂沿用此码，待契约补全后对齐
 * （修改契约需用户批准）。
 */
@Getter
public enum ErrorReason {

    /** 资源不存在或已被删除。 */
    RESOURCE_NOT_FOUND("资源不存在"),

    /** Token 缺失、无效或已过期。 */
    TOKEN_MISSING_OR_EXPIRED("未登录或登录已过期"),

    /** 账号状态不满足操作前提（如未实名认证）。 */
    AUTH_STATUS_NOT_ALLOWED("账号状态不允许此操作"),

    /** 需要管理员权限。 */
    ADMIN_REQUIRED("需要管理员权限"),

    /** 重复操作或业务状态冲突（注册重复、重复提交、学号已占用等统一归此）。 */
    DUPLICATE_OR_CONFLICTED_OPERATION("操作冲突，当前状态不允许此操作"),

    /** 实名认证审核记录已处理，不能重复审核。 */
    VERIFICATION_REVIEW_ALREADY_HANDLED("审核记录已处理，不能重复审核"),

    /**
     * 登录凭证错误（用户不存在或密码错误），不区分以防用户枚举。
     * 契约缺口：{@code api_design.yaml} 尚未定义登录失败的 reason。
     */
    INVALID_CREDENTIALS("用户名或密码错误"),

    /** 当前用户不是该代取服务的参与者（发布方/接单方），无权读取凭证等受限资源。 */
    NOT_PICKUP_PARTICIPANT("非该代取服务的参与者"),

    /** 代取服务当前状态不允许此操作（如非待接单不能接单、非进行中不能确认完成、不可取消等）。 */
    PICKUP_STATUS_NOT_ALLOWED("代取服务当前状态不允许此操作"),

    /** 接单时服务已超过接单截止时间并被流转为取消。 */
    ACCEPT_DEADLINE_EXPIRED("代取服务已超过接单截止时间"),

    /** 完成凭证尚未上传，或当前状态下不可读取完成凭证。 */
    COMPLETION_PROOF_NOT_AVAILABLE("完成凭证尚未上传或不可读取");

    /** 缺省中文提示语；调用方可用更具体的 message 覆盖。 */
    private final String defaultMessage;

    ErrorReason(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }
}
