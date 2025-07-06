package com.orv.api.domain.media;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AudioExtractServiceImpl implements AudioExtractService {
    /**
     * FFmpeg를 사용하여 비디오 파일에서 오디오를 추출합니다.
     * @param inputVideoFile 입력 비디오 파일
     * @param outputAudioFile 출력 오디오 파일
     * @param format 출력 오디오 형식 (오입력시 기본값은 MP3)
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    @Override
    public void extractAudio(File inputVideoFile, File outputAudioFile, String format) throws IOException {
        String ffmpegPath = "ffmpeg";
        ProcessBuilder processBuilder = new ProcessBuilder(
                ffmpegPath,
                "-y",                                    // 출력 파일 덮어쓰기
                "-i", inputVideoFile.getAbsolutePath(),  // 입력 비디오 파일
                "-vn",                                   // 비디오 스트림 제거
                "-acodec", getAudioCodec(format),        // 오디오 코덱 설정
                "-ar", "44100",                          // 샘플레이트 44.1kHz
                "-ac", "2",                              // 스테레오 채널
                "-ab", "192k",                           // 비트레이트 192kbps
                outputAudioFile.getAbsolutePath()        // 출력 파일
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
                throw new RuntimeException("FFmpeg 오디오 추출 실패, exit code: " + exitCode);
            }
            log.info("오디오 추출 완료: {}", outputAudioFile.getAbsolutePath());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt(); // 인터럽트 상태 복구
            throw new IOException("오디오 추출 중 인터럽트 발생", exception);
        }
    }

    /**
     * 오디오 형식에 따른 코덱을 반환합니다.
     * @param format 오디오 형식 (오입력시 기본값은 MP3)
     * @return FFmpeg 오디오 코덱
     */
    private String getAudioCodec(String format) {
        return switch (format.toLowerCase()) {
            case "mp3" -> "libmp3lame";
            case "wav" -> "pcm_s16le";
            case "aac" -> "aac";
            case "opus" -> "libopus";
            case "flac" -> "flac";
            default -> "libmp3lame"; // 기본값: MP3
        };
    }
}
