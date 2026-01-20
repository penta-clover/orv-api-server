package com.orv.api.domain.auth.service;

import org.springframework.stereotype.Service;

import com.orv.api.domain.auth.service.dto.SocialUserInfo;

@Service
public interface SocialAuthService {
    SocialUserInfo getUserInfo(String code);
    default String getAuthorizationUrl(String state) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
