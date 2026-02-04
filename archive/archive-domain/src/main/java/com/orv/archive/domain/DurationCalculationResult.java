package com.orv.archive.domain;

public record DurationCalculationResult(
    boolean success,
    int durationSeconds,
    String errorMessage
) {
    public static DurationCalculationResult success(int seconds) {
        return new DurationCalculationResult(true, seconds, null);
    }

    public static DurationCalculationResult failure(String message) {
        return new DurationCalculationResult(false, 0, message);
    }
}
