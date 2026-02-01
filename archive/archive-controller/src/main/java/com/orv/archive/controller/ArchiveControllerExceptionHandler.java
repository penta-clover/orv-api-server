package com.orv.archive.controller;

import com.orv.archive.common.ArchiveException;
import com.orv.common.dto.ApiResponse;
import com.orv.common.dto.ErrorCode;
import lombok.extern.slf4j.Slf4j;
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
