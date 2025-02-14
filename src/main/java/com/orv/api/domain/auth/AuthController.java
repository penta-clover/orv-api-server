package com.orv.api.domain.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/auth/")
public class AuthController {
    @GetMapping("/login/google")
    public void googleLogin(HttpServletResponse response) throws IOException {
        String clientId = "233929552250-v5jv4q6j2t4mun5hp82eo3kuqp8nmhol.apps.googleusercontent.com";
        String redirectUri = "https://api.orv.im/api/v0/auth/callback/google";
        String responseType = "code";
        String scope = "openid email profile";
        String state = UUID.randomUUID().toString(); // CSRF 공격 방지를 위해 랜덤값 사용 권장

        String googleOAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=" + responseType
                + "&scope=" + scope
                + "&state=" + state;

        response.sendRedirect(googleOAuthUrl);
    }

    @GetMapping("/callback/google")
    public Object googleCallback(HttpServletRequest req) {
        Map<String, Object> map = new HashMap<>();
        map.put("url", req.getRequestURL());
        return map;
    }
}
