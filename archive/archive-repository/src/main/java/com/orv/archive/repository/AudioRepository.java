package com.orv.archive.repository;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

import com.orv.archive.domain.AudioMetadata;

public interface AudioRepository {
    Optional<URI> save(InputStream inputStream, AudioMetadata audioMetadata);
}
