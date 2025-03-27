package com.orv.api.domain.reservation;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RecapRepository {
    Optional<UUID> reserveRecap(UUID memberId, UUID videoId, LocalDateTime scheduledAt);
}
