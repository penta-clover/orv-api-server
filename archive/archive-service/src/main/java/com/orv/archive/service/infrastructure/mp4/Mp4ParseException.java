package com.orv.archive.service.infrastructure.mp4;

public class Mp4ParseException extends RuntimeException {

    public Mp4ParseException(String message) {
        super(message);
    }

    public Mp4ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
