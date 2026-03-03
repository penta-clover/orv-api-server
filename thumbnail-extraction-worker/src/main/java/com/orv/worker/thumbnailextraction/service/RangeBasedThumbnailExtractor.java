package com.orv.worker.thumbnailextraction.service;

import com.orv.archive.domain.CandidateThumbnailExtractionResult;
import com.orv.archive.domain.CandidateThumbnailExtractionResult.CandidateFrame;
import com.orv.archive.domain.Video;
import com.orv.archive.repository.MeasuringFileReader;
import com.orv.archive.repository.VideoFileReader;
import com.orv.archive.repository.VideoRepository;
import com.orv.archive.service.infrastructure.ImageSharpnessCalculator;
import com.orv.archive.service.infrastructure.mp4.KeyframeInfo;
import com.orv.archive.service.infrastructure.mp4.Mp4ParseException;
import com.orv.archive.service.infrastructure.mp4.VideoTrackInfo;
import com.orv.common.aop.MeasurePerformance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
@RequiredArgsConstructor
public class RangeBasedThumbnailExtractor {

    private static final long SEGMENT_DURATION_MS = 5_000;

    private final VideoRepository videoRepository;
    private final VideoFileReader videoFileReader;
    private final Mp4MetadataFetcher mp4MetadataFetcher;
    private final List<FrameDecoder> frameDecoders;
    private final ImageSharpnessCalculator sharpnessCalculator;

    private record TimeRange(long startMs, long endMs) {}

    @MeasurePerformance("extract-candidates")
    public CandidateThumbnailExtractionResult extractCandidates(UUID videoId) {
        try {
            // 1. Validation: 처리 대상 영상이 유효한지 확인한다
            Video video = videoRepository.findById(videoId).orElse(null);
            if (video == null || video.getVideoFileKey() == null) {
                return CandidateThumbnailExtractionResult.failure("Video file key not found: " + videoId);
            }

            // 2. Fetch Metadata: 영상의 메타데이터를 불러온다
            String fileKey = video.getVideoFileKey();
            MeasuringFileReader reader = new MeasuringFileReader(videoFileReader);
            VideoTrackInfo trackInfo = mp4MetadataFetcher.fetch(reader, fileKey);
            log.info("perf operation=extract-detail codec={} resolution={}x{} video_duration_ms={}",
                    trackInfo.codecType(), trackInfo.width(), trackInfo.height(), trackInfo.durationMs());

            // 3. Divide as ranges: 썸네일을 추출한 구간(segment)을 나눈다.
            List<TimeRange> segments = computeSegments(trackInfo.durationMs());

            // 4. Calculate Candidates: 키프레임을 디코딩하고, 구간별 베스트 프레임을 계산한다.
            FrameDecoder decoder = findDecoder(trackInfo.codecType());
            List<CandidateFrame> candidates = new ArrayList<>();

            for (TimeRange segment : segments) {
                Optional<CandidateFrame> candidate = extractBestKeyframe(reader, fileKey, trackInfo, decoder, segment);
                candidate.ifPresent(candidates::add);
            }

            // 5. Return result: 썸네일 추출 결과를 반환한다
            log.info("perf operation=download video_id={} total_bytes={} download_ms={} requests={}",
                    videoId, reader.totalBytes(), reader.totalTimeMs(), reader.requestCount());

            if (candidates.isEmpty()) {
                return CandidateThumbnailExtractionResult.failure("No keyframes could be extracted");
            }

            return CandidateThumbnailExtractionResult.success(candidates);
        } catch (UnsupportedCodecException e) {
            log.error("Unsupported codec for video {}", videoId, e);
            return CandidateThumbnailExtractionResult.failure(e.getMessage());
        } catch (Mp4ParseException e) {
            log.error("MP4 parse error for video {}", videoId, e);
            return CandidateThumbnailExtractionResult.failure("MP4 parse error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to extract thumbnails for video {}", videoId, e);
            return CandidateThumbnailExtractionResult.failure(e.getMessage());
        }
    }

    private Optional<CandidateFrame> extractBestKeyframe(
            VideoFileReader reader, String fileKey, VideoTrackInfo trackInfo,
            FrameDecoder decoder, TimeRange segment) {

        List<KeyframeInfo> keyframes = trackInfo.findKeyframesInRange(segment.startMs(), segment.endMs());

        if (keyframes.isEmpty()) {
            return Optional.empty();
        }

        CandidateFrame best = null;

        for (KeyframeInfo keyframe : keyframes) {
            try {
                byte[] sampleData = reader.getRange(fileKey, keyframe.fileOffset(), keyframe.size());
                BufferedImage image = decoder.decode(
                        trackInfo.codecConfig(), sampleData, trackInfo.nalLengthSize(),
                        trackInfo.width(), trackInfo.height());
                double sharpness = sharpnessCalculator.calculate(image);
                log.debug("Keyframe at {}ms: size={}B, sharpness={}", keyframe.timestampMs(), keyframe.size(), sharpness);

                if (best == null || sharpness > best.sharpnessScore()) {
                    best = new CandidateFrame(keyframe.timestampMs(), image, sharpness);
                }
            } catch (Exception e) {
                log.warn("Failed to decode keyframe at {}ms (sample {}), skipping",
                        keyframe.timestampMs(), keyframe.sampleIndex(), e);
            }
        }

        return Optional.ofNullable(best);
    }

    private FrameDecoder findDecoder(String codecType) {
        return frameDecoders.stream()
                .filter(d -> d.supports(codecType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedCodecException("Unsupported codec: " + codecType));
    }

    private List<TimeRange> computeSegments(long durationMs) {
        if (durationMs <= SEGMENT_DURATION_MS) {
            return List.of(new TimeRange(0, durationMs));
        }

        boolean canSplitDistinctly = durationMs >= 3 * SEGMENT_DURATION_MS;

        if (!canSplitDistinctly) {
            return List.of(
                    new TimeRange(0, SEGMENT_DURATION_MS),
                    new TimeRange(durationMs - SEGMENT_DURATION_MS, durationMs)
            );
        }

        long lastSegmentStartAt = durationMs - SEGMENT_DURATION_MS;
        long midSegmentStartAt = ThreadLocalRandom.current()
                .nextLong(SEGMENT_DURATION_MS, lastSegmentStartAt - SEGMENT_DURATION_MS + 1);

        return List.of(
                new TimeRange(0, SEGMENT_DURATION_MS),
                new TimeRange(midSegmentStartAt, midSegmentStartAt + SEGMENT_DURATION_MS),
                new TimeRange(lastSegmentStartAt, durationMs)
        );
    }
}
