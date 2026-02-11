package com.orv.recap.common;

public enum RecapErrorCode {
    VIDEO_NOT_FOUND(404, "RC001", "영상을 찾을 수 없습니다."),
    RECAP_RESERVATION_FAILED(500, "RC002", "리캡 예약에 실패했습니다."),
    RECAP_RESULT_NOT_FOUND(404, "RC003", "리캡 결과를 찾을 수 없습니다."),
    RECAP_AUDIO_NOT_FOUND(404, "RC004", "리캡 오디오를 찾을 수 없습니다."),
    AUDIO_EXTRACTION_FAILED(500, "RC005", "오디오 추출에 실패했습니다.");

    private final int statusCode;
    private final String code;
    private final String message;

    RecapErrorCode(int statusCode, String code, String message) {
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
