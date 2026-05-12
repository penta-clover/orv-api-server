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
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v0/auth/")
public class AuthController {
    public static final String OAUTH_STATE_SESSION_ATTRIBUTE = "ORV_OAUTH_STATE";
    public static final String OAUTH_PROVIDER_SESSION_ATTRIBUTE = "ORV_OAUTH_PROVIDER";
    public static final String PENDING_SIGNUP_MEMBER_ID_SESSION_ATTRIBUTE = "ORV_PENDING_SIGNUP_MEMBER_ID";
    public static final String PENDING_SIGNUP_PROVIDER_SESSION_ATTRIBUTE = "ORV_PENDING_SIGNUP_PROVIDER";
    public static final String PENDING_SIGNUP_SOCIAL_ID_SESSION_ATTRIBUTE = "ORV_PENDING_SIGNUP_SOCIAL_ID";

    private final AuthOrchestrator authOrchestrator;

    @Value("${security.frontend.callback-url}")
    private String callbackUrl;

    @Value("${server.servlet.session.cookie.name:ORVSESSION}")
    private String sessionCookieName;

    @Value("${server.servlet.session.cookie.secure:true}")
    private boolean secureSessionCookie;

    @Value("${server.servlet.session.cookie.same-site:Lax}")
    private String sessionCookieSameSite;

    @GetMapping("/login/{provider}")
    public void login(@PathVariable String provider, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String state = UUID.randomUUID().toString(); // CSRF 공격 방지를 위해 랜덤값 사용
        HttpSession session = request.getSession(true);
        session.setAttribute(OAUTH_STATE_SESSION_ATTRIBUTE, state);
        session.setAttribute(OAUTH_PROVIDER_SESSION_ATTRIBUTE, provider);

        String authUrl = authOrchestrator.getAuthorizationUrl(provider, state);
        response.sendRedirect(authUrl);
    }

    @GetMapping("/callback/{provider}")
    public void callback(@PathVariable String provider, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String code = request.getParameter("code");
        String state = request.getParameter("state");
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OAuth code is required.");
        }
        validateOAuthState(provider, state, request);

        SocialUserInfo userInfo = authOrchestrator.getUserInfo(provider, code);
        Optional<Member> member = authOrchestrator.findByProviderAndSocialId(userInfo.getProvider(), userInfo.getId());

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

            setAuthenticatedSession(request, mem.getId().toString(), roles.get());
        } else {
            // 미가입 사용자
            setPendingSignupSession(request, userInfo);
        }

        String redirectUrl = callbackUrl + "?isNewUser=" + !isRegistered;
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/nicknames")
    public ApiResponse<ValidationResultResponse> validNickname(@RequestParam("nickname") String nickname) {
        ValidationResultResponse validationResult = authOrchestrator.validateNickname(nickname);
        return ApiResponse.success(validationResult, 200);
    }

    @PostMapping("/join")
    public ApiResponse<Object> join(@RequestBody JoinForm joinForm, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Pending signup session is required.");
        }

        String memberId = (String) session.getAttribute(PENDING_SIGNUP_MEMBER_ID_SESSION_ATTRIBUTE);
        String provider = (String) session.getAttribute(PENDING_SIGNUP_PROVIDER_SESSION_ATTRIBUTE);
        String socialId = (String) session.getAttribute(PENDING_SIGNUP_SOCIAL_ID_SESSION_ATTRIBUTE);

        if (memberId == null || provider == null || socialId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Pending signup session is required.");
        }

        authOrchestrator.join(memberId, joinForm.getNickname(), joinForm.getGender(), joinForm.getBirthDay(), provider, socialId, joinForm.getPhoneNumber());
        clearPendingSignupSession(session);
        setAuthenticatedSession(request, memberId, List.of());

        return ApiResponse.success(null, 200);
    }

    @PostMapping("/logout")
    public ApiResponse<Object> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        ResponseCookie expiredSessionCookie = ResponseCookie.from(sessionCookieName, "")
                .httpOnly(true)
                .secure(secureSessionCookie)
                .sameSite(sessionCookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredSessionCookie.toString());

        return ApiResponse.success(null, 200);
    }

    private void validateOAuthState(String provider, String state, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || state == null || state.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OAuth state.");
        }

        String expectedState = (String) session.getAttribute(OAUTH_STATE_SESSION_ATTRIBUTE);
        String expectedProvider = (String) session.getAttribute(OAUTH_PROVIDER_SESSION_ATTRIBUTE);
        session.removeAttribute(OAUTH_STATE_SESSION_ATTRIBUTE);
        session.removeAttribute(OAUTH_PROVIDER_SESSION_ATTRIBUTE);

        if (!state.equals(expectedState) || !provider.equalsIgnoreCase(expectedProvider)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OAuth state.");
        }
    }

    private void setPendingSignupSession(HttpServletRequest request, SocialUserInfo userInfo) {
        HttpSession session = request.getSession(true);
        changeSessionId(request);
        session.setAttribute(PENDING_SIGNUP_MEMBER_ID_SESSION_ATTRIBUTE, UUID.randomUUID().toString());
        session.setAttribute(PENDING_SIGNUP_PROVIDER_SESSION_ATTRIBUTE, userInfo.getProvider());
        session.setAttribute(PENDING_SIGNUP_SOCIAL_ID_SESSION_ATTRIBUTE, userInfo.getId());
    }

    private void setAuthenticatedSession(HttpServletRequest request, String memberId, List<Role> roles) {
        HttpSession session = request.getSession(true);
        changeSessionId(request);
        clearPendingSignupSession(session);

        List<GrantedAuthority> authorities = roles.stream()
                .map(Role::getName)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        Authentication authentication = new UsernamePasswordAuthenticationToken(memberId, null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    private void clearPendingSignupSession(HttpSession session) {
        session.removeAttribute(PENDING_SIGNUP_MEMBER_ID_SESSION_ATTRIBUTE);
        session.removeAttribute(PENDING_SIGNUP_PROVIDER_SESSION_ATTRIBUTE);
        session.removeAttribute(PENDING_SIGNUP_SOCIAL_ID_SESSION_ATTRIBUTE);
    }

    private void changeSessionId(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
    }
}
