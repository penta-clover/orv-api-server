package com.orv.api.domain.reservation;

import com.orv.api.domain.reservation.dto.RecapContent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.orv.api.domain.reservation.dto.RecapResultResponse;

public interface RecapResultRepository {
    Optional<UUID> save(UUID recapReservationId, List<RecapContent> contents);
    Optional<RecapResultResponse> findByRecapReservationId(UUID recapReservationId);
}
