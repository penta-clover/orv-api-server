package com.orv.archive.orchestrator;

import com.orv.archive.orchestrator.dto.PresignedUrlResponse;
import com.orv.archive.orchestrator.dto.ThumbnailCandidateResponse;
import com.orv.archive.orchestrator.dto.VideoResponse;
import com.orv.archive.service.ArchiveService;
import com.orv.archive.service.PublicVideoUrlGenerator;
import com.orv.archive.domain.ImageMetadata;
import com.orv.archive.domain.PresignedUrlInfo;
import com.orv.archive.domain.ThumbnailCandidate;
import com.orv.archive.domain.Video;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ArchiveOrchestrator {
    private final ArchiveService archiveService;
    private final PublicVideoUrlGenerator publicVideoUrlGenerator;

    public Optional<String> uploadRecordedVideo(InputStream inputStream, String contentType, long size, UUID storyboardId, UUID memberId) throws IOException {
        return archiveService.uploadRecordedVideo(inputStream, contentType, size, storyboardId, memberId);
    }

    public Optional<VideoResponse> getVideo(UUID videoId) {
        return archiveService.getVideo(videoId).map(this::toVideoResponse);
    }

    public List<VideoResponse> getMyVideos(UUID memberId, int offset, int limit) {
        List<Video> videos = archiveService.getMyVideos(memberId, offset, limit);
        return videos.stream()
                .map(this::toVideoResponse)
                .collect(Collectors.toList());
    }

    public boolean updateVideoTitle(UUID videoId, String title) {
        return archiveService.updateVideoTitle(videoId, title);
    }

    public boolean updateVideoThumbnail(UUID videoId, InputStream inputStream, String contentType, long size) throws IOException {
        return archiveService.updateVideoThumbnail(videoId, inputStream, new ImageMetadata(contentType, size));
    }

    // V1 API methods
    public PresignedUrlResponse requestUploadUrl(UUID storyboardId, UUID memberId) {
        PresignedUrlInfo info = archiveService.requestUploadUrl(storyboardId, memberId);
        return toPresignedUrlResponse(info);
    }

    public String confirmUpload(UUID videoId, UUID memberId) {
        return archiveService.confirmUpload(videoId, memberId);
    }

    public List<ThumbnailCandidateResponse> getThumbnailCandidates(UUID videoId) {
        return archiveService.getThumbnailCandidates(videoId).stream()
                .map(this::toThumbnailCandidateResponse)
                .collect(Collectors.toList());
    }

    public void selectThumbnailCandidate(UUID videoId, Long candidateId) {
        archiveService.selectThumbnailCandidate(videoId, candidateId);
    }

    private VideoResponse toVideoResponse(Video video) {
        return new VideoResponse(
                video.getId(),
                video.getStoryboardId(),
                video.getMemberId(),
                resolveVideoUrl(video),
                video.getCreatedAt(),
                resolveThumbnailUrl(video),
                video.getRunningTime(),
                video.getTitle(),
                video.getStatus()
        );
    }

    private String resolveVideoUrl(Video video) {
        if (video.getVideoFileKey() != null) {
            return publicVideoUrlGenerator.generateUrl(video.getVideoFileKey());
        }
        return video.getVideoUrl();
    }

    private String resolveThumbnailUrl(Video video) {
        if (video.getThumbnailFileKey() != null) {
            return publicVideoUrlGenerator.generateUrl(video.getThumbnailFileKey());
        }
        return video.getThumbnailUrl();
    }

    private ThumbnailCandidateResponse toThumbnailCandidateResponse(ThumbnailCandidate candidate) {
        return new ThumbnailCandidateResponse(
                candidate.getId(),
                candidate.getTimestampMs(),
                publicVideoUrlGenerator.generateUrl(candidate.getFileKey()),
                candidate.getCreatedAt()
        );
    }

    private PresignedUrlResponse toPresignedUrlResponse(PresignedUrlInfo info) {
        return new PresignedUrlResponse(
                info.getVideoId(),
                info.getUploadUrl(),
                info.getExpiresAt()
        );
    }
}
