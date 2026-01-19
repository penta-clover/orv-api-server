package com.orv.api.domain.media.service;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.*;
import org.bytedeco.ffmpeg.global.avcodec;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AudioExtractServiceImpl implements AudioExtractService {
    /**
     * JavaCV를 사용하여 비디오 파일에서 오디오를 추출합니다.
     * @param inputVideoFile 입력 비디오 파일
     * @param outputAudioFile 출력 오디오 파일
     * @param format 출력 오디오 형식 (오입력시 기본값은 MP3)
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    @Override
    public void extractAudio(File inputVideoFile, File outputAudioFile, String format) throws IOException {
        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;
        
        try {
            // 비디오 파일에서 프레임 추출 준비
            grabber = new FFmpegFrameGrabber(inputVideoFile);
            grabber.start();
            
            // 오디오 추출 검증
            if (grabber.getAudioChannels() == 0) {
                throw new IOException("입력 파일에 오디오 스트림이 없습니다: " + inputVideoFile.getAbsolutePath());
            }
            
            // 입력 오디오의 실제 파라미터 가져오기
            int inputChannels = grabber.getAudioChannels();
            int inputSampleRate = grabber.getSampleRate();
            
            // 출력 오디오 파일 레코더 설정 - 입력과 동일한 채널 수 사용
            recorder = new FFmpegFrameRecorder(outputAudioFile, inputChannels);
            
            // 오디오 코덱 설정
            String codecName = getAudioCodec(format);
            if (codecName.equals("pcm_s16le")) {
                // WAV 파일의 경우
                recorder.setFormat("wav");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_PCM_S16LE);
            } else if (codecName.equals("libmp3lame")) {
                // MP3 파일의 경우
                recorder.setFormat("mp3");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
            } else if (codecName.equals("aac")) {
                // AAC 파일의 경우
                recorder.setFormat("aac");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            } else if (codecName.equals("libopus")) {
                // Opus 파일의 경우
                recorder.setFormat("opus");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_OPUS);
            } else if (codecName.equals("flac")) {
                // FLAC 파일의 경우
                recorder.setFormat("flac");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_FLAC);
            } else {
                // 기본값: MP3
                recorder.setFormat("mp3");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
            }
            
            // 오디오 파라미터 설정 - 입력 소스의 파라미터 사용
            // WAV의 경우 리샘플링, 아니면 입력값 사용
            if (codecName.equals("pcm_s16le")) {
                recorder.setSampleRate(48000); // Opus 호환을 위한 48kHz
            } else {
                recorder.setSampleRate(inputSampleRate); // 입력과 동일한 샘플레이트
            }
            recorder.setAudioChannels(Math.min(inputChannels, 2));  // 입력과 동일한 채널 수
            recorder.setAudioBitrate(192000); // 비트레이트 192kbps
            recorder.setAudioQuality(0); // 최고 품질
            
            // 레코더 시작
            recorder.start();
            
            // 프레임 복사
            Frame frame;
            while ((frame = grabber.grabSamples()) != null) {
                recorder.recordSamples(frame.samples);
            }
            
            log.info("오디오 추출 완료: {}", outputAudioFile.getAbsolutePath());
            
        } catch (Exception e) {
            log.error("오디오 추출 중 오류 발생: {}", e.getMessage(), e);
            throw new IOException("오디오 추출 실패: " + e.getMessage(), e);
        } finally {
            // 리소스 정리
            try {
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                }
            } catch (Exception e) {
                log.error("레코더 종료 중 오류: {}", e.getMessage());
            }
            
            try {
                if (grabber != null) {
                    grabber.stop();
                    grabber.release();
                }
            } catch (Exception e) {
                log.error("그래버 종료 중 오류: {}", e.getMessage());
            }
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
