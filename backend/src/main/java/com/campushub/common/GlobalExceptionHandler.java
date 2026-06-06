package com.campushub.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理器，把控制器与服务层抛出的异常统一翻译成 {@link ErrorResponse}，
 * 并设置与 {@link ErrorCode#getHttpStatus()} 一致的 HTTP 状态码。
 *
 * <p>{@code @RestControllerAdvice} = {@code @ControllerAdvice} + {@code @ResponseBody}：
 * Spring 会让本类的 {@code @ExceptionHandler} 方法对所有 Controller 生效，
 * 返回值直接作为 JSON 响应体。这样业务代码只管抛异常，不必各处手写错误响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：使用其携带的错误码、消息与可选的结构化详情。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        return build(code, ex.getMessage(), ex.getErrors());
    }

    /** 请求体 {@code @Valid} 校验失败：收集字段级错误，字段名作为 errors 的 key。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return build(ErrorCode.INVALID_PARAM, ErrorCode.INVALID_PARAM.getDefaultMessage(), fieldErrors);
    }

    /** 查询参数对象绑定/校验失败（如分页参数对象）：同样按字段名汇总。 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return build(ErrorCode.INVALID_PARAM, ErrorCode.INVALID_PARAM.getDefaultMessage(), fieldErrors);
    }

    /** 方法参数级校验失败（{@code @Validated} 作用于单个 path/query 参数时）。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            fieldErrors.putIfAbsent(lastNode(violation), violation.getMessage());
        }
        return build(ErrorCode.INVALID_PARAM, ErrorCode.INVALID_PARAM.getDefaultMessage(), fieldErrors);
    }

    /** 缺少必填查询参数。 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        Map<String, String> fieldErrors = Map.of(ex.getParameterName(), "缺少必填参数");
        return build(ErrorCode.INVALID_PARAM, ErrorCode.INVALID_PARAM.getDefaultMessage(), fieldErrors);
    }

    /** 请求体无法解析（如非法 JSON、枚举值非法）。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return build(ErrorCode.INVALID_PARAM, "请求体格式错误或字段取值非法", null);
    }

    /** 兜底：未预期的服务端异常，记录日志但不向外暴露堆栈细节。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("未处理异常", ex);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), null);
    }

    /** 统一构造：HTTP 状态来自 ErrorCode，响应体为 ErrorResponse。 */
    private ResponseEntity<ErrorResponse> build(ErrorCode code, String message, Object errors) {
        ErrorResponse body = ErrorResponse.of(code, message, errors);
        return ResponseEntity.status(code.getHttpStatus()).body(body);
    }

    /** 取属性路径的最后一段作为字段名，例如 {@code publish.arg0.page} → {@code page}。 */
    private String lastNode(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int idx = path.lastIndexOf('.');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }
}
