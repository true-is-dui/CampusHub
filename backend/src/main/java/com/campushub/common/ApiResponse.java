package com.campushub.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 统一响应体，所有接口的返回都包一层本类。字段与 {@code api_design.yaml}
 * 中的 {@code ApiResponse} / {@code ErrorResponse} 契约一致。
 *
 * <p>成功与失败用同一个类表达（团队已确认的单类方案）：
 * <ul>
 *   <li>成功：{@code code=0}，{@code data} 为业务数据或 null，不输出 {@code errors}；</li>
 *   <li>失败：{@code code} 为业务错误码，{@code data} 固定 null，{@code errors} 视情况携带字段级详情。</li>
 * </ul>
 *
 * <p>序列化策略：{@code data} 必须始终出现（契约要求 data 为必填，无数据时为 null），
 * 因此 <b>不</b>在类级别使用 {@code NON_NULL}，以免成功响应把 {@code data:null} 一并吞掉。
 * 只在 {@code errors} 字段上单独标注 {@code @JsonInclude(NON_NULL)}（字段级覆盖类级默认），
 * 使成功响应不出现 {@code errors} 字段，错误响应在有详情时才带上它。
 *
 * @param <T> data 的业务数据类型
 */
@Getter
public class ApiResponse<T> {

    /** 业务状态码：0 表示成功，非 0 为 {@link ErrorCode#getCode()}。 */
    private final int code;

    /** 面向前端展示或调试的处理结果说明。 */
    private final String message;

    /** 成功数据；无数据时为 null。 */
    private final T data;

    /**
     * 结构化错误详情，仅错误响应可能携带。
     * 参数错误时以字段名为 key（如 {@code request}、{@code password}），
     * 状态类错误以 {@code reason} 为 key。值为 null 时不参与序列化，
     * 故成功响应不会出现该字段。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Object errors;

    private ApiResponse(int code, String message, T data, Object errors) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.errors = errors;
    }

    /** 成功且无数据（如纯操作类接口），data 固定 null。 */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "ok", null, null);
    }

    /** 成功并携带数据。 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data, null);
    }

    /** 失败响应：仅错误码与提示，无字段级详情。 */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null, null);
    }

    /** 失败响应：附带结构化错误详情（字段名→说明，或 reason→说明）。 */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, Object errors) {
        return new ApiResponse<>(errorCode.getCode(), message, null, errors);
    }
}
