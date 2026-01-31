package com.orv.archive.repository;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.orv.archive.domain.ImageMetadata;
import com.orv.archive.domain.Video;
import com.orv.archive.domain.VideoMetadata;

public interface VideoRepository {
    Optional<String> save(InputStream inputStream, VideoMetadata videoMetadata);

    Optional<Video> findById(UUID videoId);

    List<Video> findByMemberId(UUID memberId, int offset, int limit);

    boolean updateTitle(UUID videoId, String title);

    boolean updateThumbnail(UUID videoId, InputStream thumbnail, ImageMetadata imageMetadata);

    Optional<InputStream> getVideoStream(UUID videoId);

    List<Video> findAllByMemberId(UUID memberId);

    // v1 API methods
    String createPendingVideo(UUID storyboardId, UUID memberId);

    URL generateUploadUrl(UUID videoId, long expirationMinutes);

    boolean checkUploadComplete(UUID videoId);

    boolean updateVideoUrlAndStatus(UUID videoId, String videoUrl, String status);

    boolean deleteVideo(UUID videoId);
}
