package com.orv.auth.orchestrator;

import com.orv.auth.orchestrator.dto.MemberInfoResponse;
import com.orv.auth.orchestrator.dto.MemberProfileResponse;
import com.orv.auth.service.MemberService;
import com.orv.auth.domain.MemberInfo;
import com.orv.auth.domain.MemberProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MemberOrchestrator {
    private final MemberService memberService;

    public MemberInfoResponse getMyInfo(UUID memberId) {
        MemberInfo memberInfo = memberService.getMyInfo(memberId);
        return toMemberInfoResponse(memberInfo);
    }

    public MemberProfileResponse getProfile(UUID memberId) {
        MemberProfile memberProfile = memberService.getProfile(memberId);
        return toMemberProfileResponse(memberProfile);
    }

    private MemberInfoResponse toMemberInfoResponse(MemberInfo info) {
        return new MemberInfoResponse(
                info.getId(),
                info.getNickname(),
                info.getProfileImageUrl(),
                info.getCreatedAt()
        );
    }

    private MemberProfileResponse toMemberProfileResponse(MemberProfile profile) {
        return new MemberProfileResponse(
                profile.getId(),
                profile.getNickname(),
                profile.getProfileImageUrl(),
                profile.getCreatedAt()
        );
    }
}
