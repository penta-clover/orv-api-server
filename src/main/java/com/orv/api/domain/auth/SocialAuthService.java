package com.orv.api.domain.auth;

import java.util.Map;

public interface SocialAuthService {
    Map<String, Object> getUserInfo(String code);
    default String getAuthorizationUrl(String state) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
