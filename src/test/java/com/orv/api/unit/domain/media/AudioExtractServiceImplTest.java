package com.orv.api.unit.domain.media;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.orv.api.domain.media.service.AudioExtractService;
import com.orv.api.domain.media.service.AudioExtractServiceImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.*;

public class AudioExtractServiceImplTest {
    private File inputVideoFile;
    private File outputAudioFile;
    private AudioExtractService audioExtractService = new AudioExtractServiceImpl();

    @BeforeEach
    void setUp() throws Exception {
        inputVideoFile = File.createTempFile("test-video", ".mp4");
        try (var is = getClass().getResourceAsStream("/videos/extract-test-video.mp4");
             var os = new FileOutputStream(inputVideoFile)) {
            if (is == null) throw new IllegalStateException("extract-test-video.mp4 리소스가 없습니다.");
            is.transferTo(os);
        }

        outputAudioFile = new File(System.getProperty("java.io.tmpdir"),
                "extracted-audio-" + System.nanoTime() + ".mp3");
    }

    @AfterEach
    void tearDown() {
        if (inputVideoFile.exists()) {
            inputVideoFile.delete();
        }

        if (outputAudioFile.exists()) {
            outputAudioFile.delete();
        }
    }

    @Test
    void testExtractAudio() throws Exception {
        audioExtractService.extractAudio(inputVideoFile, outputAudioFile, "mp3");

        assertThat(outputAudioFile.exists()).isTrue();
        assertThat(Files.size(outputAudioFile.toPath())).isGreaterThan(0);
    }
}