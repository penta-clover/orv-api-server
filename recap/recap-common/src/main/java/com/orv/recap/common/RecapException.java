package com.orv.recap.common;

public class RecapException extends RuntimeException {
    private final RecapErrorCode errorCode;

    public RecapException(RecapErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public RecapException(RecapErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public RecapErrorCode getErrorCode() {
        return errorCode;
    }
}
