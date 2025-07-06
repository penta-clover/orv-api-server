package com.orv.api.domain.reservation;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RecapService {
    Optional<UUID> reserveRecap(UUID memberId, UUID videoId, ZonedDateTime scheduledAt) throws IOException;
}
