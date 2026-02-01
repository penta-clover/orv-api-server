package com.orv.archive.common;

public class ArchiveException extends RuntimeException {
    private final ArchiveErrorCode errorCode;

    public ArchiveException(ArchiveErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ArchiveErrorCode getErrorCode() {
        return errorCode;
    }
}
