package com.orv.api.domain.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Member {
    private UUID id;
    private String nickname;
    private String provider;
    private String socialId;
    private String email;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private String role;
    private String phoneNumber;
    private Integer birthYear;
    private String gender;
    private String name;
}
