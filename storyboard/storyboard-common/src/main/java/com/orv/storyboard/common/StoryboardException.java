package com.orv.storyboard.common;

public class StoryboardException extends RuntimeException {
    private final StoryboardErrorCode errorCode;

    public StoryboardException(StoryboardErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public StoryboardErrorCode getErrorCode() {
        return errorCode;
    }
}
