package com.orv.api.domain.reservation.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.orv.api.domain.media.service.dto.InterviewAudioRecording;

public interface RecapRepository {
    Optional<UUID> reserveRecap(UUID memberId, UUID videoId, LocalDateTime scheduledAt);
    void linkAudioRecording(UUID recapReservationId, UUID audioRecordingId);
    void linkRecapResult(UUID recapReservationId, UUID recapResultId);
    Optional<InterviewAudioRecording> findAudioByRecapReservationId(UUID recapReservationId);
}
