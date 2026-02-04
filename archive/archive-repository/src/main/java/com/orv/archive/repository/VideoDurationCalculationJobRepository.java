package com.orv.archive.repository;

import com.orv.archive.domain.VideoDurationCalculationJob;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface VideoDurationCalculationJobRepository {
    void create(UUID videoId);

    Optional<VideoDurationCalculationJob> claimNext(Duration stuckThreshold);

    void markCompleted(Long jobId);

    void markFailed(Long jobId);
}
