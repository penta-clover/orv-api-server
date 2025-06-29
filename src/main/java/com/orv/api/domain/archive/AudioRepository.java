package com.orv.api.domain.archive;

import com.orv.api.domain.archive.dto.AudioMetadata;

import java.io.InputStream;
import java.util.Optional;

public interface AudioRepository {
    Optional<String> save(InputStream inputStream, AudioMetadata audioMetadata);
}
