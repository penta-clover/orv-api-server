package com.orv.api.unit.domain.media;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.orv.api.domain.media.service.AudioCompressionService;
import com.orv.api.domain.media.service.AudioOpusCompressionServiceImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.*;

public class AudioOpusCompressionServiceImplTest {
    private File inputFile;
    private File outputFile;
    private AudioCompressionService compressionService = new AudioOpusCompressionServiceImpl();

    @BeforeEach
    void setUp() throws Exception {
        inputFile = File.createTempFile("test-audio", ".wav");
        try (FileOutputStream fos = new FileOutputStream(inputFile)) {
            fos.write(new byte[]{
                    'R','I','F','F', 0,0,0,0, // RIFF header + dummy size
                    'W','A','V','E',          // WAVE id
                    'f','m','t',' ', 16,0,0,0,// fmt chunk (PCM)
                    1,0, 1,0, 0x40,0x1f,0,0, 0x40,0x3e,0,0,
                    2,0, 16,0,                // ...
                    'd','a','t','a', 0,0,0,0  // empty data chunk
            });
        }

        outputFile = new File(System.getProperty("java.io.tmpdir"), "compressed-auido-" + System.nanoTime() + ".opus");
    }

    @AfterEach
    void tearDown() {
        if (inputFile.exists()) {
            inputFile.delete();
        }

        if (outputFile.exists()) {
            outputFile.delete();
        }
    }

    @Test
    void testCompress() throws Exception {
        compressionService.compress(inputFile, outputFile);

        assertThat(outputFile.exists());
        assertThat(Files.size(outputFile.toPath())).isGreaterThan(0);
    }
}
