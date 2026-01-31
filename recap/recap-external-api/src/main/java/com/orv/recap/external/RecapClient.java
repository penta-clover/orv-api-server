package com.orv.recap.external;

import java.util.Optional;

import com.orv.recap.external.dto.RecapServerRequest;
import com.orv.recap.external.dto.RecapServerResponse;

public interface RecapClient {
    Optional<RecapServerResponse> requestRecap(RecapServerRequest request);
}
