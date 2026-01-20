package com.orv.api.domain.auth.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberInfoResponse {
    private UUID id;
    private String nickname;
    private String profileImageUrl;
    private LocalDateTime createdAt;
}
