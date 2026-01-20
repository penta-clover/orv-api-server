package com.orv.api.domain.media.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.*;
import org.bytedeco.ffmpeg.global.avcodec;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OpusAudioCompressor implements AudioCompressor {
    @Override
    public void compress(File inputFile, File outputFile) throws IOException {
        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;
        
        try {
            grabber = new FFmpegFrameGrabber(inputFile);
            grabber.start();
            
            if (grabber.getAudioChannels() == 0) {
                throw new IOException("입력 파일에 오디오 스트림이 없습니다: " + inputFile.getAbsolutePath());
            }
            
            recorder = new FFmpegFrameRecorder(outputFile, grabber.getAudioChannels());
            
            recorder.setFormat("opus");
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_OPUS);
            
            int inputSampleRate = grabber.getSampleRate();
            int outputSampleRate = getOpusSupportedSampleRate(inputSampleRate);
            
            recorder.setSampleRate(outputSampleRate);
            recorder.setAudioChannels(Math.min(grabber.getAudioChannels(), 2));
            recorder.setAudioBitrate(48000);
            
            recorder.setAudioOption("b", "48000");
            recorder.setAudioOption("vbr", "2");
            recorder.setAudioOption("compression_level", "10");
            
            recorder.start();
            
            Frame frame;
            while ((frame = grabber.grabSamples()) != null) {
                recorder.recordSamples(frame.samples);
            }
            
            log.info("Opus 압축 완료: {}", outputFile.getAbsolutePath());
            
        } catch (Exception e) {
            log.error("오디오 압축 중 오류 발생: {}", e.getMessage(), e);
            throw new IOException("Opus 압축 실패: " + e.getMessage(), e);
        } finally {
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
    
    private int getOpusSupportedSampleRate(int inputSampleRate) {
        int[] supportedRates = {48000, 24000, 16000, 12000, 8000};
        
        for (int rate : supportedRates) {
            if (inputSampleRate >= rate) {
                return rate;
            }
        }
        
        return 8000;
    }
}
