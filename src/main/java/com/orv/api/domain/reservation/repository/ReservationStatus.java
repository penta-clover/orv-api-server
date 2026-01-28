package com.orv.api.domain.reservation;

public enum ReservationStatus {
    PENDING("pending"),
    DONE("done"),
    CANCELLED("cancelled");

    private final String value;

    ReservationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ReservationStatus fromValue(String value) {
        for (ReservationStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown reservation status: " + value);
    }
}
