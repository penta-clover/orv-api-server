package com.orv.reservation.common;

public class ReservationException extends RuntimeException {
    private final ReservationErrorCode errorCode;

    public ReservationException(ReservationErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ReservationErrorCode getErrorCode() {
        return errorCode;
    }
}
