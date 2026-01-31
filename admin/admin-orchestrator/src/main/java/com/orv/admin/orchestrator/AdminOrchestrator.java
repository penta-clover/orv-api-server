package com.orv.admin.orchestrator;

import com.orv.admin.orchestrator.dto.MemberResponse;
import com.orv.admin.orchestrator.dto.VideoResponse;
import com.orv.admin.service.AdminService;
import com.orv.admin.external.auth.AdminMemberApi;
import com.orv.admin.external.archive.AdminVideoApi;
import com.orv.archive.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AdminOrchestrator {
    private final AdminService adminService;
    private final ArchiveService archiveService;

    public boolean deleteVideo(UUID videoId) {
        return archiveService.deleteVideo(videoId);
    }

    public List<MemberResponse> getMembersByProvider(String provider) {
        List<AdminMemberApi.MemberInfo> members = adminService.getMembersByProvider(provider);
        return members.stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    public List<VideoResponse> getVideosByMemberId(UUID memberId) {
        List<AdminVideoApi.VideoInfo> videos = adminService.getVideosByMemberId(memberId);
        return videos.stream()
                .map(this::toVideoResponse)
                .collect(Collectors.toList());
    }

    private MemberResponse toMemberResponse(AdminMemberApi.MemberInfo member) {
        return new MemberResponse(
                member.getId(),
                member.getNickname(),
                member.getProvider(),
                member.getSocialId(),
                null,  // email not available in API
                null,  // profileImageUrl not available
                null,  // createdAt not available
                null,  // phoneNumber not available
                null,  // birthday not available
                null,  // gender not available
                null   // name not available
        );
    }

    private VideoResponse toVideoResponse(AdminVideoApi.VideoInfo video) {
        return new VideoResponse(
                video.getId(),
                null,  // storyboardId not available in API
                video.getMemberId(),
                null,  // videoUrl not available
                null,  // createdAt not available
                null,  // thumbnailUrl not available
                null,  // runningTime not available
                video.getTitle(),
                video.getStatus()
        );
    }
}
