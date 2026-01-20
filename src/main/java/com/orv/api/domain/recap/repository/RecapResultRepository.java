package com.orv.api.domain.recap.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.orv.api.domain.recap.service.dto.RecapContent;
import com.orv.api.domain.recap.controller.dto.RecapResultResponse;

public interface RecapResultRepository {
    Optional<UUID> save(UUID recapReservationId, List<RecapContent> contents);
    Optional<RecapResultResponse> findByRecapReservationId(UUID recapReservationId);
}
