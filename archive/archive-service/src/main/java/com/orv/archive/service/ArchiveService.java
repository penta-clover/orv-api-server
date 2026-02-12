package com.orv.archive.service;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.orv.archive.domain.ImageMetadata;
import com.orv.archive.domain.PresignedUrlInfo;
import com.orv.archive.domain.ThumbnailCandidate;
import com.orv.archive.domain.Video;

public interface ArchiveService {
    Optional<String> uploadRecordedVideo(InputStream videoStream, String contentType, long size, UUID storyboardId, UUID memberId);

    Optional<Video> getVideo(UUID videoId);

    Optional<InputStream> getVideoStream(UUID videoId);

    List<Video> getMyVideos(UUID memberId, int page, int pageSize);

    boolean updateVideoTitle(UUID videoId, String title);

    boolean updateVideoThumbnail(UUID videoId, InputStream thumbnailStream, ImageMetadata metadata);

    boolean deleteVideo(UUID videoId);

    // v1 API methods
    PresignedUrlInfo requestUploadUrl(UUID storyboardId, UUID memberId);

    String confirmUpload(UUID videoId, UUID memberId);

    List<ThumbnailCandidate> getThumbnailCandidates(UUID videoId);

    void selectThumbnailCandidate(UUID videoId, Long candidateId);
}
