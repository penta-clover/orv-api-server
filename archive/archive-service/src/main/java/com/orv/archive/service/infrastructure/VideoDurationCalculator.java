package com.orv.archive.service.infrastructure;

import com.orv.archive.domain.DurationCalculationResult;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
public class VideoDurationCalculator {

    private static final String VIDEO_FORMAT = "mp4";
    private static final double MICROSECONDS_PER_SECOND = 1_000_000.0;

    public DurationCalculationResult calculate(File videoFile) {
        FFmpegFrameGrabber grabber = null;
        try {
            grabber = new FFmpegFrameGrabber(videoFile);
            grabber.setFormat(VIDEO_FORMAT);
            grabber.start();

            long lengthInTime = grabber.getLengthInTime();
            int seconds;

            if (lengthInTime > 0) {
                seconds = (int) (lengthInTime / MICROSECONDS_PER_SECOND);
            } else {
                seconds = calculateByFrameTraversal(grabber);
            }

            if (seconds <= 0) {
                return DurationCalculationResult.failure("Invalid duration: " + seconds);
            }

            return DurationCalculationResult.success(seconds);

        } catch (Exception e) {
            log.error("Failed to calculate duration from {}", videoFile.getName(), e);
            return DurationCalculationResult.failure(e.getMessage());
        } finally {
            FFmpegUtils.safeClose(grabber);
        }
    }

    private int calculateByFrameTraversal(FFmpegFrameGrabber grabber) {
        try {
            Frame frame;
            long firstTimestamp = -1;
            long lastTimestamp = -1;

            while ((frame = grabber.grabFrame()) != null) {
                if (frame.timestamp > 0) {
                    if (firstTimestamp == -1) {
                        firstTimestamp = frame.timestamp;
                    }
                    lastTimestamp = frame.timestamp;
                }
            }

            if (firstTimestamp != -1 && lastTimestamp != -1) {
                return (int) ((lastTimestamp - firstTimestamp) / MICROSECONDS_PER_SECOND);
            }

            return 0;
        } catch (Exception e) {
            log.warn("Failed to calculate duration by frame traversal", e);
            return 0;
        }
    }
}
