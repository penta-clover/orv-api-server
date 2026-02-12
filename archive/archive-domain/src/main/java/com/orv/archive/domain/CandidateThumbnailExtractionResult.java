package com.orv.archive.domain;

import java.awt.image.BufferedImage;
import java.util.List;

public record CandidateThumbnailExtractionResult(
    boolean success,
    List<CandidateFrame> candidates,
    String errorMessage
) {
    public record CandidateFrame(
        long timestampMs,
        BufferedImage image,
        double sharpnessScore
    ) {}

    public static CandidateThumbnailExtractionResult success(List<CandidateFrame> candidates) {
        return new CandidateThumbnailExtractionResult(true, candidates, null);
    }

    public static CandidateThumbnailExtractionResult failure(String message) {
        return new CandidateThumbnailExtractionResult(false, List.of(), message);
    }
}
