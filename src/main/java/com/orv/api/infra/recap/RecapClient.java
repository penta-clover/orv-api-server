package com.orv.api.infra.recap;

import java.util.Optional;

import com.orv.api.domain.reservation.controller.dto.RecapServerRequest;
import com.orv.api.domain.reservation.controller.dto.RecapServerResponse;

public interface RecapClient {
    Optional<RecapServerResponse> requestRecap(RecapServerRequest request);
}
