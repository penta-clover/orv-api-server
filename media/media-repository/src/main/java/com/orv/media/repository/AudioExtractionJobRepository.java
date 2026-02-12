package com.orv.media.repository;

import com.orv.media.domain.AudioExtractionJob;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface AudioExtractionJobRepository {
    void create(UUID videoId, UUID recapReservationId, UUID memberId, UUID storyboardId);

    Optional<AudioExtractionJob> claimNext(Duration stuckThreshold);

    void markCompleted(Long jobId, UUID resultAudioRecordingId);

    void markFailed(Long jobId);
}
