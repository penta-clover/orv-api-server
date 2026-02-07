package com.orv.storyboard.domain;

public enum StoryboardUsageStatus {
    STARTED("STARTED"),
    COMPLETED("COMPLETED");

    private final String value;

    StoryboardUsageStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
