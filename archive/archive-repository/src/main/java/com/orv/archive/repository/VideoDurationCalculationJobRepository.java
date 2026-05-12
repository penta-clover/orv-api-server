package com.orv.archive.repository;

import com.orv.archive.domain.ClaimedArchiveJob;
import com.orv.archive.domain.JobStatus;
import com.orv.archive.domain.VideoDurationCalculationJob;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface VideoDurationCalculationJobRepository {
    void create(UUID videoId);

    Optional<ClaimedArchiveJob<VideoDurationCalculationJob>> claimNext(Duration stuckThreshold);

    boolean markCompleted(Long jobId);

    boolean markFailed(Long jobId);

    void resetToPreClaimState(Long jobId, JobStatus previousStatus, LocalDateTime previousStartedAt);
}
