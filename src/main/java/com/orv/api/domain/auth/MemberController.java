package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.MemberInfo;
import com.orv.api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v0/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/my-info")
    public ApiResponse getMyInfo() {
        UUID myId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        MemberInfo myInfo = memberService.getMyInfo(myId);
        return ApiResponse.success(myInfo, 200);
    }
}
