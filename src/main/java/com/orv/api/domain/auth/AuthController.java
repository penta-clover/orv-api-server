package com.orv.api.domain.auth;

import com.orv.api.global.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v0/auth/")
public class AuthController {
    private final SocialAuthServiceFactory socialAuthServiceFactory;

    @GetMapping("/login/{provider}")
    public void googleLogin(@PathVariable String provider, HttpServletResponse response) throws IOException {
        String state = UUID.randomUUID().toString(); // CSRF 공격 방지를 위해 랜덤값 사용

        SocialAuthService socialAuthService = socialAuthServiceFactory.getSocialAuthService(provider);
        String authUrl = socialAuthService.getAuthorizationUrl(state);

        response.sendRedirect(authUrl);
    }

    @GetMapping("/callback/{provider}")
    public Object googleCallback(@PathVariable String provider, HttpServletRequest req) {
        String code = req.getParameter("code");

        SocialAuthService socialAuthService = socialAuthServiceFactory.getSocialAuthService(provider);
        SocialUserInfo userInfo = socialAuthService.getUserInfo(code);
        return ApiResponse.success(userInfo, 200);
    }
}
