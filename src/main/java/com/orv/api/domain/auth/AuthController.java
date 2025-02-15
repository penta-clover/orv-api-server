package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.Member;
import com.orv.api.domain.auth.dto.SocialUserInfo;
import com.orv.api.global.dto.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v0/auth/")
public class AuthController {
    private final SocialAuthServiceFactory socialAuthServiceFactory;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${security.frontend.callback-url}")
    private String callbackUrl;

    @GetMapping("/login/{provider}")
    public void login(@PathVariable String provider, HttpServletResponse response) throws IOException {
        String state = UUID.randomUUID().toString(); // CSRF 공격 방지를 위해 랜덤값 사용

        SocialAuthService socialAuthService = socialAuthServiceFactory.getSocialAuthService(provider);
        String authUrl = socialAuthService.getAuthorizationUrl(state);

        response.sendRedirect(authUrl);
    }

    @GetMapping("/callback/{provider}")
    public void callback(@PathVariable String provider, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String code = request.getParameter("code");

        SocialAuthService socialAuthService = socialAuthServiceFactory.getSocialAuthService(provider);
        SocialUserInfo userInfo = socialAuthService.getUserInfo(code);

        Optional<Member> member = memberRepository.findByProviderAndSocialId(userInfo.getProvider(), userInfo.getId());

        String token;
        boolean isRegistered = member.isPresent();

        if (isRegistered) {
            // 가입된 사용자
            Member mem = member.get();
            token = jwtTokenProvider.createToken(mem.getId(), Map.of("provider", mem.getProvider(), "socialId", mem.getSocialId()));
        } else {
            // 미가입 사용자
            String temporaryId = UUID.randomUUID().toString();
            token = jwtTokenProvider.createToken(temporaryId, Map.of("provider", userInfo.getProvider(), "socialId", userInfo.getId()));
        }

        String redirectUrl = callbackUrl + "?isNewUser=" + !isRegistered + "&jwtToken=" + token;
        response.sendRedirect(redirectUrl);
    }
}
