package com.orv.api.domain.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v0/auth/")
public class AuthController {
    @GetMapping("/login/google")
    public void googleLogin(HttpServletResponse response) throws IOException {
        String clientId = "1058421128761-u2okga6ipnkaesqgt20h5al98l4cdpvm.apps.googleusercontent.com";
        String redirectUri = "https://orv.rightning.lol/api/v0/auth/callback/google";
        String responseType = "code";
        String scope = "openid email profile";
        String state = "RANDOM_STATE_VALUE"; // CSRF 공격 방지를 위해 랜덤값 사용 권장

        String googleOAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=" + responseType
                + "&scope=" + scope
                + "&state=" + state;

        response.sendRedirect(googleOAuthUrl);
    }

    @GetMapping("/callback/google")
    public String googleCallback() {
        return "HI...";
    }
}
