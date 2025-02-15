package com.orv.api.domain.auth.dto;

import lombok.Data;

@Data
public class Member {
    private String id;
    private String nickname;
    private String provider;
    private String socialId;
    private String email;
}
