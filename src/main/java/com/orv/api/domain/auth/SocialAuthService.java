package com.orv.api.domain.auth;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface SocialAuthService {
    SocialUserInfo getUserInfo(String code);
    default String getAuthorizationUrl(String state) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
