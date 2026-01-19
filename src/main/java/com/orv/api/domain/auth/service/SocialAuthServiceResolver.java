package com.orv.api.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialAuthServiceResolver {
    private final GoogleAuthService googleAuthService;
    private final KakaoAuthService kakaoAuthService;
    private final Optional<TestAuthService> testAuthService;

    public SocialAuthService getSocialAuthService(String provider) {
        if ("google".equalsIgnoreCase(provider)) {
            return googleAuthService;
        } else if ("kakao".equalsIgnoreCase(provider)) {
            return kakaoAuthService;
        } else if ("test".equalsIgnoreCase(provider)) {
            return testAuthService.orElseThrow(() -> 
                new IllegalArgumentException("테스트 인증 서비스가 활성화되지 않았습니다."));
        } else {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다: " + provider);
        }
    }
}
