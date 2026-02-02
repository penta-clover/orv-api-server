package com.orv.archive.repository;

import com.orv.archive.domain.VideoThumbnailExtractionJob;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface VideoThumbnailExtractionJobRepository {
    void create(UUID videoId);

    Optional<VideoThumbnailExtractionJob> claimNext(Duration stuckThreshold);

    void markCompleted(Long jobId);

    void markFailed(Long jobId);
}
