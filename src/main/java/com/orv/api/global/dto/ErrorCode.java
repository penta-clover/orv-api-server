package com.orv.api.global.dto;

public enum ErrorCode {
    UNKNOWN(500, "A000", "Unknown server error occured"),
    NOT_FOUND(404, "A001", "Resource not found");

    private final int statusCode;
    private final String tag;
    private final String message;


    ErrorCode(final int statusCode, final String tag, final String message) {
        this.statusCode = statusCode;
        this.tag = tag;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getTag() {
        return tag;
    }

    public String getMessage() {
        return message;
    }
}
