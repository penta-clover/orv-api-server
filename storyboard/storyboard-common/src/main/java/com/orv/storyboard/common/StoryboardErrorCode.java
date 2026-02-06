package com.orv.storyboard.common;

public enum StoryboardErrorCode {
    PARTICIPATION_LIMIT_EXCEEDED(400, "SB001", "참여 인원이 마감되었습니다."),
    STORYBOARD_NOT_FOUND(404, "SB002", "스토리보드를 찾을 수 없습니다.");

    private final int statusCode;
    private final String code;
    private final String message;

    StoryboardErrorCode(int statusCode, String code, String message) {
        this.statusCode = statusCode;
        this.code = code;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
