package com.orv.api.domain.auth.service.dto;

import lombok.Data;

@Data
public class SocialUserInfo {
    private String provider;
    private String id;
    private String nickname;
    private String email;
}
