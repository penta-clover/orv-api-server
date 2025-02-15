package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.SocialUserInfo;
import org.springframework.stereotype.Service;

@Service
public interface SocialAuthService {
    SocialUserInfo getUserInfo(String code);
    default String getAuthorizationUrl(String state) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
