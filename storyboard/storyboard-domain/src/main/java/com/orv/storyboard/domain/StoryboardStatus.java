package com.orv.storyboard.domain;

public enum StoryboardStatus {
    ACTIVE("ACTIVE"),
    READ_ONLY("READ_ONLY"),
    DELETED("DELETED");

    private final String value;

    StoryboardStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StoryboardStatus fromValue(String value) {
        for (StoryboardStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown storyboard status: " + value);
    }
}
