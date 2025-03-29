package com.orv.api.global.bizgo;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlimtalkService {
    private final OmniTokenService omniTokenService;
    private final RestTemplate restTemplate;

    @Value("${bizgo.omni.client.baseurl}")
    private String baseUrl;

    @Value("${bizgo.omni.client.senderkey}")
    private String senderKey;

    public String sendAlimtalk(String phoneNumber, String templateCode, String title, String text) throws Exception{
        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + omniTokenService.getToken());

        Map<String, String> payload = new HashMap<>();
        payload.put("senderKey", senderKey);
        payload.put("msgType", "AT");
        payload.put("to", phoneNumber);
        payload.put("templateCode", templateCode);
        payload.put("title", title);
        payload.put("text", text);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<SendingResponse> response = restTemplate.exchange(
                baseUrl + "/v1/send/alimtalk",
                HttpMethod.POST,
                entity,
                SendingResponse.class
        );

        boolean isSuccessful = response.getStatusCode().is2xxSuccessful() && response.getBody().code.equals("A000");

        if (!isSuccessful) {
            throw new Exception("Failed to send alimtalk: " + response.getBody().result);
        }

        return response.getBody().msgKey;
    }

    @Data
    public static class SendingResponse {
        private String code;
        private String result;
        private String msgKey;
    }
}
