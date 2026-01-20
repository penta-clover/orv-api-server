package com.orv.api.domain.recap.infrastructure;

import java.util.Optional;

import com.orv.api.domain.recap.infrastructure.dto.RecapServerRequest;
import com.orv.api.domain.recap.infrastructure.dto.RecapServerResponse;

public interface RecapClient {
    Optional<RecapServerResponse> requestRecap(RecapServerRequest request);
}
