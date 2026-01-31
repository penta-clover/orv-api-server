package com.orv.auth.external;

import org.springframework.stereotype.Service;

import com.orv.auth.domain.SocialUserInfo;

@Service
public interface SocialAuthService {
    SocialUserInfo getUserInfo(String code);
    default String getAuthorizationUrl(String state) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
