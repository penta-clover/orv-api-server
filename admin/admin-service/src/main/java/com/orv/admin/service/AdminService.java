package com.orv.admin.service;

import com.orv.admin.external.auth.AdminMemberApi;
import com.orv.admin.external.archive.AdminVideoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminMemberApi memberApi;
    private final AdminVideoApi videoApi;

    public List<AdminMemberApi.MemberInfo> getMembersByProvider(String provider) {
        return memberApi.getMembersByProvider(provider);
    }

    public List<AdminVideoApi.VideoInfo> getVideosByMemberId(UUID memberId) {
        return videoApi.getVideosByMemberId(memberId);
    }
}
