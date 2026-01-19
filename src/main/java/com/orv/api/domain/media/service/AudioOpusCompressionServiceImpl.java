package com.orv.api.domain.media.service;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.*;
import org.bytedeco.ffmpeg.global.avcodec;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AudioOpusCompressionServiceImpl implements AudioCompressionService {
    /**
     * JavaCV를 사용하여 오디오 파일을 Opus 코덱으로 압축합니다.
     * @param inputFile the input audio file to be compressed
     * @param outputFile the output file where the compressed audio will be saved
     */
    @Override
    public void compress(File inputFile, File outputFile) throws IOException {
        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;
        
        try {
            // 입력 오디오 파일에서 프레임 추출 준비
            grabber = new FFmpegFrameGrabber(inputFile);
            grabber.start();
            
            // 오디오 스트림 확인
            if (grabber.getAudioChannels() == 0) {
                throw new IOException("입력 파일에 오디오 스트림이 없습니다: " + inputFile.getAbsolutePath());
            }
            
            // 출력 파일 레코더 설정 (Opus 형식)
            recorder = new FFmpegFrameRecorder(outputFile, grabber.getAudioChannels());
            
            // Opus 코덱 설정
            recorder.setFormat("opus");
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_OPUS);
            
            // Opus가 지원하는 샘플레이트 중 가장 가까운 값 선택
            int inputSampleRate = grabber.getSampleRate();
            int outputSampleRate = getOpusSupportedSampleRate(inputSampleRate);
            
            // 오디오 파라미터 설정
            recorder.setSampleRate(outputSampleRate); // Opus 호환 샘플레이트
            recorder.setAudioChannels(Math.min(grabber.getAudioChannels(), 2)); // Opus는 최대 2채널 지원
            recorder.setAudioBitrate(48000); // 48kbps (Opus 권장 비트레이트)
            
            // Opus 특정 옵션 설정
            recorder.setAudioOption("b", "48000");  // 비트레이트 명시
            recorder.setAudioOption("vbr", "2");    // VBR 모드 (0=CBR, 1=VBR, 2=constrained VBR)
            recorder.setAudioOption("compression_level", "10"); // 압축 레벨
            
            // 레코더 시작
            recorder.start();
            
            // 오디오 프레임 복사
            Frame frame;
            while ((frame = grabber.grabSamples()) != null) {
                recorder.recordSamples(frame.samples);
            }
            
            log.info("Opus 압축 완료: {}", outputFile.getAbsolutePath());
            
        } catch (Exception e) {
            log.error("오디오 압축 중 오류 발생: {}", e.getMessage(), e);
            throw new IOException("Opus 압축 실패: " + e.getMessage(), e);
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
     * Opus 코덱이 지원하는 샘플레이트 중 입력 샘플레이트와 가장 가까운 값을 반환합니다.
     * Opus 지원 샘플레이트: 48000, 24000, 16000, 12000, 8000
     * @param inputSampleRate 입력 샘플레이트
     * @return Opus가 지원하는 가장 가까운 샘플레이트
     */
    private int getOpusSupportedSampleRate(int inputSampleRate) {
        // Opus가 지원하는 샘플레이트
        int[] supportedRates = {48000, 24000, 16000, 12000, 8000};
        
        // 입력값 이상인 첫 번째 지원 샘플레이트 반환
        for (int rate : supportedRates) {
            if (inputSampleRate >= rate) {
                return rate;
            }
        }
        
        // 입력값이 8000Hz 미만인 경우 8000Hz 반환
        return 8000;
    }
}
