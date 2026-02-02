package com.orv.archive.service.infrastructure;

import com.orv.archive.domain.ThumbnailExtractionResult;
import com.orv.archive.service.VideoProcessingUtils;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;

@Component
@Slf4j
public class VideoThumbnailExtractor {

    public ThumbnailExtractionResult extract(File videoFile) {
        FFmpegFrameGrabber grabber = null;
        try {
            grabber = new FFmpegFrameGrabber(videoFile);
            grabber.setFormat("mp4");
            grabber.start();

            Optional<BufferedImage> keyFrame = VideoProcessingUtils.extractKeyFrame(grabber);

            if (keyFrame.isEmpty()) {
                return ThumbnailExtractionResult.failure("Failed to extract key frame");
            }

            return ThumbnailExtractionResult.success(keyFrame.get());

        } catch (Exception e) {
            log.error("Failed to extract thumbnail from {}", videoFile.getName(), e);
            return ThumbnailExtractionResult.failure(e.getMessage());
        } finally {
            safeClose(grabber);
        }
    }

    private void safeClose(FFmpegFrameGrabber grabber) {
        if (grabber != null) {
            try {
                grabber.stop();
            } catch (Exception e) {
                log.warn("Failed to stop FFmpegFrameGrabber", e);
            }
            try {
                grabber.close();
            } catch (Exception e) {
                log.warn("Failed to close FFmpegFrameGrabber", e);
            }
        }
    }
}
