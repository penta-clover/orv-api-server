package com.orv.archive.common;

public enum ArchiveErrorCode {
    VIDEO_NOT_FOUND(404, "V001", "Video not found"),
    VIDEO_ACCESS_DENIED(403, "V002", "No permission to access this video"),
    VIDEO_STATUS_NOT_PENDING(409, "V003", "Video is not in PENDING status"),
    VIDEO_FILE_NOT_UPLOADED(422, "V004", "Video file has not been uploaded to storage"),
    VIDEO_STATUS_UPDATE_FAILED(500, "V005", "Failed to update video status"),
    INVALID_VIDEO_ID_FORMAT(400, "V006", "Invalid video ID format"),
    INVALID_STORYBOARD_ID_FORMAT(400, "V007", "Invalid storyboard ID format"),
    MISSING_REQUIRED_FIELD(400, "V008", "Required field is missing");

    private final int statusCode;
    private final String code;
    private final String message;

    ArchiveErrorCode(int statusCode, String code, String message) {
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
