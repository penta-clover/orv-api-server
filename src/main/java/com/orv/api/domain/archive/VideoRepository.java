package com.orv.api.domain.archive;

import com.orv.api.domain.archive.dto.ImageMetadata;
import com.orv.api.domain.archive.dto.Video;
import com.orv.api.domain.archive.dto.VideoMetadata;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public interface VideoRepository {
    Optional<String> save(InputStream inputStream, VideoMetadata videoMetadata);

    Optional<Video> findById(UUID videoId);

    boolean updateTitle(UUID videoId, String title);

    boolean updateThumbnail(UUID videoId, InputStream thumbnail, ImageMetadata imageMetadata);
}
