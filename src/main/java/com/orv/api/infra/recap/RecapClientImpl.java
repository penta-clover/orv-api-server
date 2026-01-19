package com.orv.api.infra.recap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.orv.api.domain.reservation.controller.dto.RecapServerRequest;
import com.orv.api.domain.reservation.controller.dto.RecapServerResponse;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecapClientImpl implements RecapClient {

    @Qualifier("longTimeoutRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${recap.server.base-url}")
    private String recapServerBaseUrl;

    @Value("${recap.server.api-key}")
    private String recapServerApiKey;

    @Override
    public Optional<RecapServerResponse> requestRecap(RecapServerRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + recapServerApiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<RecapServerRequest> requestEntity = new HttpEntity<>(request, headers);

        try {
            log.info("Calling Recap Server API");
            ResponseEntity<RecapServerResponse> response = restTemplate.exchange(
                    recapServerBaseUrl + "/api/v1/recap",
                    HttpMethod.POST,
                    requestEntity,
                    RecapServerResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to get recap from server. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                return Optional.empty();
            }

            log.info("Successfully received recap from server.");
            return Optional.ofNullable(response.getBody());

        } catch (Exception e) {
            log.error("Error calling Recap Server API: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
