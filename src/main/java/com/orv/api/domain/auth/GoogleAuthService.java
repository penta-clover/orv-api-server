package com.orv.api.domain.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GoogleAuthService implements SocialAuthService {
    @Value("${social.google.client-id}")
    private String clientId;

    @Value("${social.google.client-secret}")
    private String clientSecret;

    @Value("${social.google.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    public SocialUserInfo getUserInfo(String code) {
        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
        tokenParams.add("code", code);
        tokenParams.add("client_id", clientId);
        tokenParams.add("client_secret", clientSecret);
        tokenParams.add("redirect_uri", redirectUri);
        tokenParams.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(tokenParams, tokenHeaders);
        ResponseEntity<Map> tokenResponseEntity = restTemplate.postForEntity(
                "https://oauth2.googleapis.com/token",
                tokenRequest,
                Map.class
        );

        if (!tokenResponseEntity.getStatusCode().is2xxSuccessful() ||
            tokenResponseEntity.getBody() == null ||
            tokenResponseEntity.getBody().get("access_token") == null) {
            throw new RuntimeException("access token을 받아오지 못했습니다");
        }

        Map<String, Object> tokenResponse = tokenResponseEntity.getBody();
        String accessToken = (String) tokenResponse.get("access_token");

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        HttpEntity<String> userInfoRequest = new HttpEntity<>(userHeaders);

        ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v3/userinfo",
                HttpMethod.GET,
                userInfoRequest,
                Map.class
        );

        if (!userInfoResponse.getStatusCode().is2xxSuccessful() || userInfoResponse.getBody() == null) {
            throw new RuntimeException("사용자 정보를 불러오는 데 실패했습니다.");
        }

        Map<String, Object> body = userInfoResponse.getBody();

        SocialUserInfo userInfo = new SocialUserInfo();
        userInfo.setProvider("google");
        userInfo.setId((String) body.get("sub"));
        userInfo.setNickname((String) body.get("name"));
        userInfo.setEmail((String) body.get("email"));

        return userInfo;
    }

    @Override
    public String getAuthorizationUrl(String state) {
        String scope = "openid email profile";
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=" + scope
                + "&state=" + state;
    }
}
