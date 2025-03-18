package com.orv.api.domain.archive;

import com.orv.api.domain.archive.dto.Video;
import com.orv.api.domain.archive.dto.VideoMetadata;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public interface VideoRepository {
    Optional<String> save(InputStream inputStream, VideoMetadata videoMetadata);

    Optional<Video> findById(UUID videoId);

    boolean updateTitle(String videoId, String title);

    boolean updateThumbnail(InputStream inputStream, String videoId);
}
