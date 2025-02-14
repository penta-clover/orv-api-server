package com.orv.api.domain.auth;

import lombok.Data;

@Data
public class SocialUserInfo {
    private String provider;
    private String id;
    private String nickname;
    private String email;
}
