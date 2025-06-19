package com.orv.api.domain.media;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
public class AudioOpusCompressionServiceImpl implements AudioCompressionService {
    /**
     * Compresses an audio file using the Opus codec.
     * @param inputFile the input audio file to be compressed
     * @param outputFile the output file where the compressed audio will be saved
     */
    public void compress(File inputFile, File outputFile) throws IOException {
        String ffmpegPath = "ffmpeg";
        ProcessBuilder processBuilder = new ProcessBuilder(
                ffmpegPath,
                "-y",
                "-i", inputFile.getAbsolutePath(),
                "-vn",
                "-c:a", "libopus",
                "-b:a", "48k",
                "-vbr", "on",
                "-compression_level", "10",
                outputFile.getAbsolutePath()
        );

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;

            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg 변환 실패, exit code: " + exitCode);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt(); // 인터럽트 상태 복구
        }
    }
}
