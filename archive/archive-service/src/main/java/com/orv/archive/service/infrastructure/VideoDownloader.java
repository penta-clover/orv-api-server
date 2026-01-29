package com.orv.archive.service.infrastructure;

import com.orv.archive.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class VideoDownloader {

    private final VideoRepository videoRepository;

    public File download(UUID videoId) throws IOException {
        Optional<InputStream> streamOpt = videoRepository.getVideoStream(videoId);
        if (streamOpt.isEmpty()) {
            throw new IOException("Video not found: " + videoId);
        }

        File tempFile = File.createTempFile("duration-calculate-", ".mp4");
        try (InputStream stream = streamOpt.get()) {
            Files.copy(stream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        log.debug("Downloaded video {} to temporary file: {}", videoId, tempFile.getAbsolutePath());
        return tempFile;
    }

    public void deleteSafely(File file) {
        if (file != null && file.exists()) {
            try {
                Files.deleteIfExists(file.toPath());
                log.debug("Deleted temporary file: {}", file.getAbsolutePath());
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", file.getAbsolutePath(), e);
                file.deleteOnExit();
            }
        }
    }
}
