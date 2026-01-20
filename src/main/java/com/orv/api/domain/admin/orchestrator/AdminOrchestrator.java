package com.orv.api.domain.admin.orchestrator;

import com.orv.api.domain.admin.controller.dto.MemberResponse;
import com.orv.api.domain.admin.controller.dto.VideoResponse;
import com.orv.api.domain.admin.service.AdminService;
import com.orv.api.domain.archive.service.ArchiveService;
import com.orv.api.domain.archive.service.dto.Video;
import com.orv.api.domain.auth.service.dto.Member;
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
        List<Member> members = adminService.getMembersByProvider(provider);
        return members.stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    public List<VideoResponse> getVideosByMemberId(UUID memberId) {
        List<Video> videos = adminService.getVideosByMemberId(memberId);
        return videos.stream()
                .map(this::toVideoResponse)
                .collect(Collectors.toList());
    }

    private MemberResponse toMemberResponse(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getNickname(),
                member.getProvider(),
                member.getSocialId(),
                member.getEmail(),
                member.getProfileImageUrl(),
                member.getCreatedAt(),
                member.getPhoneNumber(),
                member.getBirthday(),
                member.getGender(),
                member.getName()
        );
    }

    private VideoResponse toVideoResponse(Video video) {
        return new VideoResponse(
                video.getId(),
                video.getStoryboardId(),
                video.getMemberId(),
                video.getVideoUrl(),
                video.getCreatedAt(),
                video.getThumbnailUrl(),
                video.getRunningTime(),
                video.getTitle(),
                video.getStatus()
        );
    }
}
