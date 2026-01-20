package com.orv.api.domain.auth.orchestrator;

import com.orv.api.domain.auth.controller.dto.ValidationResultResponse;
import com.orv.api.domain.auth.service.JwtTokenService;
import com.orv.api.domain.auth.service.MemberService;
import com.orv.api.domain.auth.service.SocialAuthService;
import com.orv.api.domain.auth.service.SocialAuthServiceResolver;
import com.orv.api.domain.auth.service.dto.Member;
import com.orv.api.domain.auth.service.dto.Role;
import com.orv.api.domain.auth.service.dto.SocialUserInfo;
import com.orv.api.domain.auth.service.dto.ValidationResult;
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
