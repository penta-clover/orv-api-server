package com.orv.api.domain.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SocialAuthServiceFactory {
    private final GoogleAuthService googleAuthService;

    public SocialAuthService getSocialAuthService(String provider) {
        if ("google".equalsIgnoreCase(provider)) {
            return googleAuthService;
        } else {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다: " + provider);
        }
    }
}
