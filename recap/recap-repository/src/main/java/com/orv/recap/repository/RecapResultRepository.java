package com.orv.recap.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.orv.recap.domain.RecapContent;
import com.orv.recap.domain.RecapResultInfo;

public interface RecapResultRepository {
    Optional<UUID> save(UUID recapReservationId, List<RecapContent> contents);
    Optional<RecapResultInfo> findByRecapReservationId(UUID recapReservationId);
}
