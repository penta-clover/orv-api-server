package com.orv.api.domain.reservation.service;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import com.orv.api.domain.reservation.service.dto.RecapAudioInfo;
import com.orv.api.domain.reservation.service.dto.RecapResultInfo;

public interface RecapService {
    Optional<UUID> reserveRecap(UUID memberId, UUID videoId, ZonedDateTime scheduledAt) throws IOException;
    Optional<RecapResultInfo> getRecapResult(UUID recapReservationId);
    Optional<RecapAudioInfo> getRecapAudio(UUID recapReservationId);
}
