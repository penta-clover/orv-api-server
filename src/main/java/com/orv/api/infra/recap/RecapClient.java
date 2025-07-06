package com.orv.api.infra.recap;

import com.orv.api.domain.reservation.dto.RecapServerRequest;
import com.orv.api.domain.reservation.dto.RecapServerResponse;

import java.util.Optional;

public interface RecapClient {
    Optional<RecapServerResponse> requestRecap(RecapServerRequest request);
}
