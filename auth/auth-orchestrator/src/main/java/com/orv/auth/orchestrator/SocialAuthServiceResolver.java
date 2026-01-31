package com.orv.auth.orchestrator;

import com.orv.auth.external.SocialAuthService;
import com.orv.auth.external.rest.GoogleAuthService;
import com.orv.auth.external.rest.KakaoAuthService;
import com.orv.auth.external.rest.TestAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
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
