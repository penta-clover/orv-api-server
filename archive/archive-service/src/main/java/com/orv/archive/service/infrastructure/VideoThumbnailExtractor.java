package com.orv.archive.service.infrastructure;

import com.orv.archive.domain.CandidateThumbnailExtractionResult;
import com.orv.archive.domain.CandidateThumbnailExtractionResult.CandidateFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Component;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
@RequiredArgsConstructor
public class VideoThumbnailExtractor {

    private static final String VIDEO_FORMAT = "mp4";
    private static final long SEGMENT_DURATION_MS = 5_000;

    private final ImageSharpnessCalculator sharpnessCalculator;

    private record TimeRange(long startMs, long endMs) {}

    public CandidateThumbnailExtractionResult extractCandidates(File videoFile) {
        FFmpegFrameGrabber grabber = null;
        try {
            grabber = new FFmpegFrameGrabber(videoFile);
            grabber.setFormat(VIDEO_FORMAT);
            grabber.start();

            long durationMs = grabber.getLengthInTime() / 1_000;
            if (durationMs <= 0) {
                return CandidateThumbnailExtractionResult.failure("Cannot determine video duration");
            }

            List<CandidateFrame> candidates = extractBestPerSegment(grabber, computeSegments(durationMs));

            if (candidates.isEmpty()) {
                return CandidateThumbnailExtractionResult.failure("No frames could be extracted");
            }
            return CandidateThumbnailExtractionResult.success(candidates);

        } catch (Exception e) {
            log.error("Failed to extract candidate thumbnails from {}", videoFile.getName(), e);
            return CandidateThumbnailExtractionResult.failure(e.getMessage());
        } finally {
            FFmpegUtils.safeClose(grabber);
        }
    }

    private List<CandidateFrame> extractBestPerSegment(
            FFmpegFrameGrabber grabber, List<TimeRange> segments) throws Exception {
        List<CandidateFrame> candidates = new ArrayList<>();
        Java2DFrameConverter converter = new Java2DFrameConverter();
        try {
            for (TimeRange segment : segments) {
                extractBestKeyFrame(grabber, converter, segment)
                        .ifPresent(candidates::add);
            }
        } finally {
            converter.close();
        }
        return candidates;
    }

    private Optional<CandidateFrame> extractBestKeyFrame(
            FFmpegFrameGrabber grabber, Java2DFrameConverter converter, TimeRange segment) throws Exception {
        grabber.setTimestamp(segment.startMs() * 1_000);

        CandidateFrame best = null;
        CandidateFrame fallback = null;

        while (true) {
            Frame frame = grabber.grabImage();
            if (frame == null) break;

            long frameMs = grabber.getTimestamp() / 1_000;
            if (frameMs > segment.endMs()) break;
            if (frameMs < segment.startMs() || frame.image == null) continue;

            BufferedImage image = converter.convert(frame);
            if (image == null) continue;

            if (!frame.keyFrame) {
                if (fallback == null) {
                    BufferedImage copy = deepCopy(image);
                    double sharpness = sharpnessCalculator.calculate(copy);
                    fallback = new CandidateFrame(frameMs, copy, sharpness);
                }
                continue;
            }

            BufferedImage copy = deepCopy(image);
            double sharpness = sharpnessCalculator.calculate(copy);

            log.debug("Keyframe at {}ms in [{}-{}ms], sharpness={}",
                    frameMs, segment.startMs(), segment.endMs(), sharpness);

            if (best == null || sharpness > best.sharpnessScore()) {
                best = new CandidateFrame(frameMs, copy, sharpness);
            }
        }

        if (best != null) return Optional.of(best);

        if (fallback != null) {
            log.warn("No keyframes in [{}-{}ms], using fallback frame at {}ms",
                    segment.startMs(), segment.endMs(), fallback.timestampMs());
            return Optional.of(fallback);
        }

        log.warn("No frames found in segment [{}-{}ms]", segment.startMs(), segment.endMs());
        return Optional.empty();
    }

    private List<TimeRange> computeSegments(long durationMs) {
        if (durationMs <= SEGMENT_DURATION_MS) {
            return List.of(new TimeRange(0, durationMs));
        }

        if (durationMs < 3 * SEGMENT_DURATION_MS) {
            return List.of(
                    new TimeRange(0, SEGMENT_DURATION_MS),
                    new TimeRange(durationMs - SEGMENT_DURATION_MS, durationMs)
            );
        }

        long endStart = durationMs - SEGMENT_DURATION_MS;
        long middleStart = ThreadLocalRandom.current()
                .nextLong(SEGMENT_DURATION_MS, endStart - SEGMENT_DURATION_MS + 1);

        return List.of(
                new TimeRange(0, SEGMENT_DURATION_MS),
                new TimeRange(middleStart, middleStart + SEGMENT_DURATION_MS),
                new TimeRange(endStart, durationMs)
        );
    }

    private static BufferedImage deepCopy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = copy.getGraphics();
        try {
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return copy;
    }
}