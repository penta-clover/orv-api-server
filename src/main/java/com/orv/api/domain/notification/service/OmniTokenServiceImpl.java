package com.orv.api.domain.notification.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class OmniTokenServiceImpl implements OmniTokenService {
    private final RestTemplate restTemplate;

    @Value("${bizgo.omni.client.id}")
    private String clientId;

    @Value("${bizgo.omni.client.passwd}")
    private String clientPasswd;

    @Value("${bizgo.omni.client.baseurl}")
    private String baseUrl;

    private String currentToken;
    private Instant tokenExpiry;


    @Override
    public synchronized String getToken() {
        if (currentToken == null || Instant.now().isAfter(tokenExpiry.minus(Duration.ofMinutes(5)))) {
            refreshToken();
        }

        return currentToken;
    }

    private void refreshToken() {
        // HTTP Header에 클라이언트 식별자와 비밀번호 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-IB-Client-Id", clientId);
        headers.set("X-IB-Client-Passwd", clientPasswd);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<TokenResponse> response = restTemplate.exchange(
                baseUrl + "/v1/auth/token",
                HttpMethod.POST,
                entity,
                TokenResponse.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            TokenResponse tokenResponse = response.getBody();
            currentToken = tokenResponse.getData().getToken();
            tokenExpiry = tokenResponse.getData().getExpired().toInstant();
        } else {
            throw new RuntimeException("Failed to get token from Bizgo");
        }
    }

    @Scheduled(fixedDelay = 12 * 60 * 60 * 1000)
    public void scheduledTokenRefresh() {
        refreshToken();
    }

    @Data
    public static class TokenResponse {
        private String code;
        private String result;
        private TokenResponseData data;
    }
    @Data
    public static class TokenResponseData {
        private String token;
        private String schema;
        private OffsetDateTime expired;
    }
}
