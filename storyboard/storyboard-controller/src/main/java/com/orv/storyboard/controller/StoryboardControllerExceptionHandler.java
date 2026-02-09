package com.orv.storyboard.controller;

import com.orv.storyboard.common.StoryboardException;
import com.orv.common.dto.ApiResponse;
import com.orv.common.dto.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(assignableTypes = StoryboardController.class)
public class StoryboardControllerExceptionHandler {

    @ExceptionHandler(StoryboardException.class)
    public ApiResponse<?> handleStoryboardException(StoryboardException e) {
        var errorCode = e.getErrorCode();
        return new ApiResponse<>(
                Integer.toString(errorCode.getStatusCode()),
                e.getMessage(),
                errorCode
        );
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
    }
}
