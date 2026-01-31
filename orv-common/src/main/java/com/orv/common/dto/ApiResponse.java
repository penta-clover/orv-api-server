package com.orv.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ApiResponse<T> {
    private String statusCode;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T response, int statusCode) {
        return new ApiResponse<>(Integer.toString(statusCode), "success", response);
    }

    public static ApiResponse<ErrorCode> fail(ErrorCode e, int statusCode) {
        return new ApiResponse<>(Integer.toString(statusCode), "fail", e);
    }
}
