package com.orv.auth.controller;

import com.orv.auth.orchestrator.dto.ValidationResultResponse;
import com.orv.auth.orchestrator.AuthOrchestrator;
import com.orv.auth.domain.JoinForm;
import com.orv.auth.domain.Member;
import com.orv.auth.domain.Role;
import com.orv.auth.domain.SocialUserInfo;
import com.orv.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v0/auth/")
public class AuthController {
    private final AuthOrchestrator authOrchestrator;

    @Value("${security.frontend.callback-url}")
    private String callbackUrl;

    @GetMapping("/login/{provider}")
    public void login(@PathVariable String provider, HttpServletResponse response) throws IOException {
        String state = UUID.randomUUID().toString(); // CSRF 공격 방지를 위해 랜덤값 사용
        String authUrl = authOrchestrator.getAuthorizationUrl(provider, state);
        response.sendRedirect(authUrl);
    }

    @GetMapping("/callback/{provider}")
    public void callback(@PathVariable String provider, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String code = request.getParameter("code");

        SocialUserInfo userInfo = authOrchestrator.getUserInfo(provider, code);
        Optional<Member> member = authOrchestrator.findByProviderAndSocialId(userInfo.getProvider(), userInfo.getId());

        String token;
        boolean isRegistered = member.isPresent();

        if (isRegistered) {
            // 가입된 사용자
            Member mem = member.get();
            Optional<List<Role>> roles = authOrchestrator.findRolesById(mem.getId());

            if (roles.isEmpty()) {
                // 권한 조회에 실패한 경우
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve user roles.");
                return;
            }

            token = authOrchestrator.createToken(
                    mem.getId().toString(),
                    Map.of("provider", mem.getProvider(),
                            "socialId", mem.getSocialId(),
                            "roles", roles.get().stream().map(Role::getName).collect(Collectors.toList())));
        } else {
            // 미가입 사용자
            String temporaryId = UUID.randomUUID().toString();
            token = authOrchestrator.createToken(
                    temporaryId,
                    Map.of("provider", userInfo.getProvider(), "socialId", userInfo.getId(), "roles", List.of()));
        }

        String redirectUrl = callbackUrl + "?isNewUser=" + !isRegistered + "&jwtToken=" + token;
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/nicknames")
    public ApiResponse<ValidationResultResponse> validNickname(@RequestParam("nickname") String nickname) {
        ValidationResultResponse validationResult = authOrchestrator.validateNickname(nickname);
        return ApiResponse.success(validationResult, 200);
    }

    @PostMapping("/join")
    public ApiResponse<Object> join(@RequestBody JoinForm joinForm, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        Map<String, ?> payload = authOrchestrator.getPayload(token);
        String provider = (String) payload.get("provider");
        String socialId = (String) payload.get("socialId");
        String memberId = SecurityContextHolder.getContext().getAuthentication().getName();

        authOrchestrator.join(memberId, joinForm.getNickname(), joinForm.getGender(), joinForm.getBirthDay(), provider, socialId, joinForm.getPhoneNumber());

        return ApiResponse.success(null, 200);
    }
}
