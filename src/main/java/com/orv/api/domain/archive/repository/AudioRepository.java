package com.orv.api.domain.archive.repository;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

import com.orv.api.domain.archive.service.dto.AudioMetadata;

public interface AudioRepository {
    Optional<URI> save(InputStream inputStream, AudioMetadata audioMetadata);
}
