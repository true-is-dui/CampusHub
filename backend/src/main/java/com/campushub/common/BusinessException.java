package com.campushub.common;

import lombok.Getter;

/**
 * 业务异常：当请求在语法上合法、但违反业务规则时由 Service 层抛出，
 * 例如重复接单、在错误状态下完成订单、越权操作他人资源等。
 *
 * <p>携带一个 {@link ErrorCode} 决定对外的业务码与 HTTP 状态，
 * 由 {@link GlobalExceptionHandler} 统一捕获并翻译成 {@link ApiResponse}，
 * 因此 Service / Controller 中无需手写错误响应的拼装。
 *
 * <p>继承 {@link RuntimeException}（非受检异常），这样 Service 方法签名
 * 不必到处 {@code throws}，且抛出后会让当前 {@code @Transactional} 事务回滚。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 决定对外业务码与 HTTP 状态的错误码。 */
    private final transient ErrorCode errorCode;

    /**
     * 结构化错误详情，可选。状态类错误通常用 {@code reason} 作 key，
     * 例如 {@code {"reason": "订单不在待接单状态"}}；为 null 时响应不含 errors。
     */
    private final transient Object errors;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BusinessException(ErrorCode errorCode, String message, Object errors) {
        super(message);
        this.errorCode = errorCode;
        this.errors = errors;
    }
}
