package com.orv.api.domain.archive;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.orv.api.domain.archive.dto.ImageMetadata;
import com.orv.api.domain.archive.dto.Video;

public interface ArchiveService {
    Optional<String> uploadRecordedVideo(InputStream videoStream, String contentType, long size, UUID storyboardId, UUID memberId);
    
    Optional<Video> getVideo(UUID videoId);
    
    List<Video> getMyVideos(UUID memberId, int page, int pageSize);
    
    boolean updateVideoTitle(UUID videoId, String title);
    
    boolean updateVideoThumbnail(UUID videoId, InputStream thumbnailStream, ImageMetadata metadata);
}
