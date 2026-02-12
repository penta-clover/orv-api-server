package com.orv.archive.domain;

import java.awt.image.BufferedImage;

public record ThumbnailExtractionResult(
    boolean success,
    BufferedImage thumbnail,
    String errorMessage
) {
    public static ThumbnailExtractionResult success(BufferedImage thumbnail) {
        return new ThumbnailExtractionResult(true, thumbnail, null);
    }

    public static ThumbnailExtractionResult failure(String message) {
        return new ThumbnailExtractionResult(false, null, message);
    }
}
