package com.orv.api.domain.notification.service;

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

import com.orv.api.domain.reservation.service.dto.AlimtalkContent;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlimtalkServiceImpl implements AlimtalkService {
    private final OmniTokenService omniTokenService;
    private final RestTemplate restTemplate;

    @Value("${bizgo.omni.client.baseurl}")
    private String baseUrl;

    @Value("${bizgo.omni.client.senderkey}")
    private String senderKey;

    @Override
    public String sendAlimtalk(AlimtalkContent alimtalkContent) throws Exception {
        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + omniTokenService.getToken());

        Map<String, Object> payload = new HashMap<>();
        payload.put("senderKey", senderKey);
        payload.put("msgType", alimtalkContent.getMsgType());
        payload.put("to", alimtalkContent.getTo());
        payload.put("templateCode", alimtalkContent.getTemplateCode());

        if (alimtalkContent.getTitle() != null) {
            payload.put("title", alimtalkContent.getTitle());
        }

        if (alimtalkContent.getButtons() != null) {
            payload.put("button", alimtalkContent.getButtons());
        }

        if (alimtalkContent.getText() != null) {
            payload.put("text", alimtalkContent.getText());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<SendingResponse> response = restTemplate.exchange(
                baseUrl + "/v1/send/alimtalk",
                HttpMethod.POST,
                entity,
                SendingResponse.class);

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