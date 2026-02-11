package com.orv.media.infrastructure.ffmpeg;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.*;
import org.bytedeco.ffmpeg.global.avcodec;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Component;
import com.orv.media.infrastructure.AudioExtractor;
@Slf4j
@Component
public class FFmpegAudioExtractor implements AudioExtractor {
    @Override
    public int extractAudio(File inputVideoFile, File outputAudioFile, String format) throws IOException {
        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;
        
        try {
            grabber = new FFmpegFrameGrabber(inputVideoFile);
            grabber.start();
            
            if (grabber.getAudioChannels() == 0) {
                throw new IOException("입력 파일에 오디오 스트림이 없습니다: " + inputVideoFile.getAbsolutePath());
            }
            
            int inputChannels = grabber.getAudioChannels();
            int inputSampleRate = grabber.getSampleRate();
            
            recorder = new FFmpegFrameRecorder(outputAudioFile, inputChannels);
            
            String codecName = getAudioCodec(format);
            if (codecName.equals("pcm_s16le")) {
                recorder.setFormat("wav");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_PCM_S16LE);
            } else if (codecName.equals("libmp3lame")) {
                recorder.setFormat("mp3");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
            } else if (codecName.equals("aac")) {
                recorder.setFormat("aac");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            } else if (codecName.equals("libopus")) {
                recorder.setFormat("opus");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_OPUS);
            } else if (codecName.equals("flac")) {
                recorder.setFormat("flac");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_FLAC);
            } else {
                recorder.setFormat("mp3");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
            }
            
            if (codecName.equals("pcm_s16le")) {
                recorder.setSampleRate(48000); 
            } else {
                recorder.setSampleRate(inputSampleRate);
            }
            recorder.setAudioChannels(Math.min(inputChannels, 2));
            recorder.setAudioBitrate(192000);
            recorder.setAudioQuality(0);
            
            recorder.start();
            
            Frame frame;
            while ((frame = grabber.grabSamples()) != null) {
                recorder.recordSamples(frame.samples);
            }
            
            int durationSeconds = (int) (grabber.getLengthInTime() / 1_000_000);
            log.info("오디오 추출 완료: {}, duration={}s", outputAudioFile.getAbsolutePath(), durationSeconds);
            return durationSeconds;

        } catch (Exception e) {
            log.error("오디오 추출 중 오류 발생: {}", e.getMessage(), e);
            throw new IOException("오디오 추출 실패: " + e.getMessage(), e);
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

    private String getAudioCodec(String format) {
        return switch (format.toLowerCase()) {
            case "mp3" -> "libmp3lame";
            case "wav" -> "pcm_s16le";
            case "aac" -> "aac";
            case "opus" -> "libopus";
            case "flac" -> "flac";
            default -> "libmp3lame";
        };
    }
}
