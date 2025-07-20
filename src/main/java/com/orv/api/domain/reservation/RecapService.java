package com.orv.api.domain.reservation;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import com.orv.api.domain.reservation.dto.RecapResultResponse;
import com.orv.api.domain.reservation.dto.RecapAudioResponse;

public interface RecapService {
    Optional<UUID> reserveRecap(UUID memberId, UUID videoId, ZonedDateTime scheduledAt) throws IOException;
    Optional<RecapResultResponse> getRecapResult(UUID recapReservationId);
    Optional<RecapAudioResponse> getRecapAudio(UUID recapReservationId);
}
