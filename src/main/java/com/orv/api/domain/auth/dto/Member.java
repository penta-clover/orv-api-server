package com.orv.api.domain.auth.dto;

import lombok.Data;

@Data
public class Member {
    private String id;
    private String provider;
    private String socialId;
}
