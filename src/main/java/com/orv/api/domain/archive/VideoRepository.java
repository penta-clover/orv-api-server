package com.orv.api.domain.archive;

import com.orv.api.domain.archive.dto.VideoMetadata;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

public interface VideoRepository {
    Optional<String> save(InputStream inputStream, VideoMetadata videoMetadata);
}
