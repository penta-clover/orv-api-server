package com.orv.archive.controller;

import com.orv.archive.common.ArchiveErrorCode;
import com.orv.archive.common.ArchiveException;
import com.orv.common.dto.ApiResponse;
import com.orv.common.dto.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(assignableTypes = ArchiveControllerV1.class)
public class ArchiveControllerExceptionHandler {

    @ExceptionHandler(ArchiveException.class)
    public ApiResponse<?> handleArchiveException(ArchiveException e) {
        var errorCode = e.getErrorCode();
        return new ApiResponse<>(
                Integer.toString(errorCode.getStatusCode()),
                e.getMessage(),
                errorCode
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse(ArchiveErrorCode.MISSING_REQUIRED_FIELD.getMessage());
        log.warn("Validation failed: {}", message);
        return new ApiResponse<>(
                Integer.toString(ArchiveErrorCode.MISSING_REQUIRED_FIELD.getStatusCode()),
                message,
                ArchiveErrorCode.MISSING_REQUIRED_FIELD
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("Missing required parameter: {}", e.getParameterName());
        return new ApiResponse<>(
                Integer.toString(ArchiveErrorCode.MISSING_REQUIRED_FIELD.getStatusCode()),
                e.getParameterName() + "는 필수입니다",
                ArchiveErrorCode.MISSING_REQUIRED_FIELD
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        return ApiResponse.fail(ErrorCode.UNKNOWN, 400);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
    }
}
