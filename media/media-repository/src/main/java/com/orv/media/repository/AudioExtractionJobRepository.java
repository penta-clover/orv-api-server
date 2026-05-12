package com.orv.media.repository;

import com.orv.media.domain.AudioExtractionJobStatus;
import com.orv.media.domain.ClaimedAudioJob;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AudioExtractionJobRepository {
    void create(UUID videoId, UUID recapReservationId, UUID memberId, UUID storyboardId);

    Optional<ClaimedAudioJob> claimNext(Duration stuckThreshold);

    boolean markCompleted(Long jobId, UUID resultAudioRecordingId);

    boolean markFailed(Long jobId);

    void resetToPreClaimState(Long jobId, AudioExtractionJobStatus previousStatus, LocalDateTime previousStartedAt);
}
