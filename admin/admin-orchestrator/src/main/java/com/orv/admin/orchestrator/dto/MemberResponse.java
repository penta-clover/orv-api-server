package com.orv.admin.orchestrator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberResponse {
    private UUID id;
    private String nickname;
    private String provider;
    private String socialId;
    private String email;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private String phoneNumber;
    private LocalDate birthday;
    private String gender;
    private String name;
}
