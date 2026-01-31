package com.orv.auth.orchestrator;

import com.orv.auth.orchestrator.dto.ValidationResultResponse;
import com.orv.auth.service.JwtTokenService;
import com.orv.auth.service.MemberService;
import com.orv.auth.external.SocialAuthService;
import com.orv.auth.domain.Member;
import com.orv.auth.domain.Role;
import com.orv.auth.domain.SocialUserInfo;
import com.orv.auth.domain.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthOrchestrator {
    private final SocialAuthServiceResolver socialAuthServiceResolver;
    private final MemberService memberService;
    private final JwtTokenService jwtTokenService;

    public String getAuthorizationUrl(String provider, String state) {
        SocialAuthService socialAuthService = socialAuthServiceResolver.getSocialAuthService(provider);
        return socialAuthService.getAuthorizationUrl(state);
    }

    public SocialUserInfo getUserInfo(String provider, String code) {
        SocialAuthService socialAuthService = socialAuthServiceResolver.getSocialAuthService(provider);
        return socialAuthService.getUserInfo(code);
    }

    public Optional<Member> findByProviderAndSocialId(String provider, String socialId) {
        return memberService.findByProviderAndSocialId(provider, socialId);
    }

    public Optional<List<Role>> findRolesById(UUID memberId) {
        return memberService.findRolesById(memberId);
    }

    public String createToken(String subject, Map<String, ?> claims) {
        return jwtTokenService.createToken(subject, claims);
    }

    public Map<String, ?> getPayload(String token) {
        return jwtTokenService.getPayload(token);
    }

    public void join(String memberId, String nickname, String gender, LocalDate birthday, String provider, String socialId, String phoneNumber) {
        memberService.join(memberId, nickname, gender, birthday, provider, socialId, phoneNumber);
    }

    public ValidationResultResponse validateNickname(String nickname) {
        ValidationResult result = memberService.validateNickname(nickname);
        return toValidationResultResponse(result);
    }

    private ValidationResultResponse toValidationResultResponse(ValidationResult result) {
        return new ValidationResultResponse(
                result.getNickname(),
                result.getIsExists(),
                result.getIsValid()
        );
    }
}
