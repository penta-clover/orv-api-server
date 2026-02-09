package com.orv.reservation.common;

public enum ReservationErrorCode {
    RESERVATION_NOT_FOUND(404, "RS001", "예약을 찾을 수 없습니다."),
    RESERVATION_ALREADY_USED(400, "RS002", "이미 사용된 예약입니다."),
    STORYBOARD_NOT_AVAILABLE(403, "RS003", "참여할 수 없는 스토리보드입니다.");

    private final int statusCode;
    private final String code;
    private final String message;

    ReservationErrorCode(int statusCode, String code, String message) {
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
