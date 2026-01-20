package com.orv.api.domain.auth.controller;

import com.orv.api.domain.auth.controller.dto.MemberInfoResponse;
import com.orv.api.domain.auth.controller.dto.MemberProfileResponse;
import com.orv.api.domain.auth.orchestrator.MemberOrchestrator;
import com.orv.api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v0/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberOrchestrator memberOrchestrator;

    @GetMapping("/my-info")
    public ApiResponse getMyInfo() {
        UUID myId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        MemberInfoResponse myInfo = memberOrchestrator.getMyInfo(myId);
        return ApiResponse.success(myInfo, 200);
    }

    @GetMapping("/{memberId}/profile")
    public ApiResponse getMemberProfile(@PathVariable UUID memberId) {
        MemberProfileResponse memberProfile = memberOrchestrator.getProfile(memberId);
        return ApiResponse.success(memberProfile, 200);
    }
}
