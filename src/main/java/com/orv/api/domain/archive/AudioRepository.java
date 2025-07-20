package com.orv.api.domain.archive;

import com.orv.api.domain.archive.dto.AudioMetadata;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

public interface AudioRepository {
    Optional<URI> save(InputStream inputStream, AudioMetadata audioMetadata);
}
