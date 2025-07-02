package com.orv.api.domain.reservation;

import java.io.IOException;
import java.util.UUID;

public interface RecapService {
    void processRecap(UUID videoId, UUID memberId) throws IOException;
}
