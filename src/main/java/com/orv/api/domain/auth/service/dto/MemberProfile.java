package com.orv.api.domain.auth.service.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MemberProfile {
    private UUID id;
    private String nickname;
    private String profileImageUrl;
    private LocalDateTime createdAt;
}
