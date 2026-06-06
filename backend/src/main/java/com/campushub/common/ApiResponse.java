package com.campushub.common;

import lombok.Getter;

/**
 * 成功响应体，对应 {@code api_design.yaml} 中的 {@code ApiResponse} schema。
 *
 * <p>仅用于成功响应：{@code code=0}，{@code data} 为业务数据或 null。
 * 该 schema 的 required 不含 {@code errors}，故本类不设 errors 字段。
 * 错误响应由独立的 {@link ErrorResponse} 表达（其 required 含 errors）。
 *
 * <p>序列化策略：三个字段一律输出，不加 {@code @JsonInclude}，
 * 以保证 {@code data} 必填、无数据时输出 {@code data:null}。
 *
 * @param <T> data 的业务数据类型
 */
@Getter
public class ApiResponse<T> {

    /** 业务状态码：成功固定为 0。 */
    private final int code;

    /** 面向前端展示或调试的处理结果说明。 */
    private final String message;

    /** 成功数据；无数据时为 null。 */
    private final T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功且无数据（如纯操作类接口），data 固定 null。 */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "ok", null);
    }

    /** 成功并携带数据。 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }
}
