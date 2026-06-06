package com.campushub.common;

import lombok.Getter;

/**
 * 错误响应体，对应 {@code api_design.yaml} 中的 {@code ErrorResponse} schema。
 *
 * <p>与成功响应 {@link ApiResponse} 分开建模，因为两个 schema 的 required 字段不同：
 * 成功响应不含 {@code errors}，而错误响应把 {@code errors} 列为 required（值可为 null，
 * 但键必须出现）。本类四个字段一律参与序列化、不加 {@code @JsonInclude}，
 * 从而保证 {@code data} 固定输出 null、{@code errors} 即使无详情也输出 null。
 *
 * <p>错误响应是 {@link GlobalExceptionHandler} 的唯一出口，业务代码只管抛
 * {@link BusinessException}，不直接构造本类。
 */
@Getter
public class ErrorResponse {

    /** 业务错误码，取值见 {@link ErrorCode#getCode()}；非 0。 */
    private final int code;

    /** 面向前端展示或调试的错误说明。 */
    private final String message;

    /** 错误响应固定为 null，仅为满足契约中 data 必填而保留该键。 */
    private final Object data;

    /**
     * 结构化错误详情。参数错误时以字段名为 key（如 {@code password}），
     * 状态类错误以 {@code reason} 为 key；无详情时为 null。
     * 契约要求该键必须出现，故不加 {@code @JsonInclude}。
     */
    private final Object errors;

    private ErrorResponse(int code, String message, Object errors) {
        this.code = code;
        this.message = message;
        this.data = null;
        this.errors = errors;
    }

    /** 错误响应：仅错误码与提示，无字段级详情（errors 输出为 null）。 */
    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message, null);
    }

    /** 错误响应：附带结构化错误详情（字段名→说明，或 reason→说明）。 */
    public static ErrorResponse of(ErrorCode errorCode, String message, Object errors) {
        return new ErrorResponse(errorCode.getCode(), message, errors);
    }
}
