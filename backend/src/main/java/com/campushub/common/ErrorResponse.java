package com.campushub.common;

import lombok.Data;

import java.util.Map;

@Data
public class ErrorResponse {
    private int code;
    private String message;
    private Object data;
    private Map<String, String> errors;

    public static ErrorResponse of(int code, String message, Map<String, String> errors) {
        ErrorResponse r = new ErrorResponse();
        r.setCode(code);
        r.setMessage(message);
        r.setData(null);
        r.setErrors(errors);
        return r;
    }

    public static ErrorResponse of(int code, String message, String reasonKey, String reasonValue) {
        return of(code, message, Map.of(reasonKey, reasonValue));
    }
}
