package com.campushub.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse handleBusiness(BusinessException e) {
        String message = e.getMessage();
        if (e.getCode() == 40001) {
            return ErrorResponse.of(40001, message, "request", message);
        }
        return ErrorResponse.of(e.getCode(), message, "reason", message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ErrorResponse.of(40001, "参数校验失败", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse handleConstraint(ConstraintViolationException e) {
        return ErrorResponse.of(40001, "参数校验失败", "request", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse handleGeneral(Exception e) {
        return ErrorResponse.of(50001, "系统内部异常", "reason", e.getMessage());
    }
}
