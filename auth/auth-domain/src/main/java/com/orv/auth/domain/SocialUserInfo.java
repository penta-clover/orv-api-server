package com.orv.auth.domain;

import lombok.Data;

@Data
public class SocialUserInfo {
    private String provider;
    private String id;
    private String nickname;
    private String email;
}
