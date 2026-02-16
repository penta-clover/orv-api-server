package com.orv.worker.thumbnailextraction.service;

import com.orv.archive.domain.CandidateThumbnailExtractionResult;
import com.orv.archive.domain.CandidateThumbnailExtractionResult.CandidateFrame;
import com.orv.archive.domain.Video;
import com.orv.archive.repository.MeasuringFileReader;
import com.orv.archive.repository.VideoFileReader;
import com.orv.archive.repository.VideoRepository;
import com.orv.archive.service.infrastructure.ImageSharpnessCalculator;
import com.orv.archive.service.infrastructure.mp4.KeyframeInfo;
import com.orv.archive.service.infrastructure.mp4.Mp4BoxHeader;
import com.orv.archive.service.infrastructure.mp4.Mp4ParseException;
import com.orv.archive.service.infrastructure.mp4.Mp4Parser;
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

/**
 * S3 Range GET 기반 썸네일 후보 추출기.
 * MP4 컨테이너를 바이트 레벨에서 파싱하여 필요한 키프레임 데이터만 다운로드한다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RangeBasedThumbnailExtractor {

    private static final int INITIAL_PROBE_SIZE = 4096;
    private static final long SEGMENT_DURATION_MS = 5_000;

    private final VideoRepository videoRepository;
    private final VideoFileReader videoFileReader;
    private final List<FrameDecoder> frameDecoders;
    private final ImageSharpnessCalculator sharpnessCalculator;

    private record TimeRange(long startMs, long endMs) {}

    @MeasurePerformance("extract-candidates")
    public CandidateThumbnailExtractionResult extractCandidates(UUID videoId) {
        try {
            // 1. 파일 키 조회
            Video video = videoRepository.findById(videoId).orElse(null);

            if (video == null || video.getVideoFileKey() == null) {
                return CandidateThumbnailExtractionResult.failure("Video file key not found: " + videoId);
            }

            String fileKey = video.getVideoFileKey();
            MeasuringFileReader reader = new MeasuringFileReader(videoFileReader);

            // 2. moov box 위치 탐색 및 다운로드
            VideoTrackInfo trackInfo = fetchAndParseMetadata(reader, fileKey);
            log.info("perf operation=extract-detail codec={} resolution={}x{} video_duration_ms={}",
                    trackInfo.codecType(), trackInfo.width(), trackInfo.height(), trackInfo.durationMs());

            // 3. 적합한 디코더 선택
            FrameDecoder decoder = findDecoder(trackInfo.codecType());

            // 4. 세그먼트 결정
            List<TimeRange> segments = computeSegments(trackInfo.durationMs());

            // 5. 세그먼트별 최고 sharpness 키프레임 추출
            List<CandidateFrame> candidates = new ArrayList<>();

            for (TimeRange segment : segments) {
                Optional<CandidateFrame> candidate = extractBestKeyframe(reader, fileKey, trackInfo, decoder, segment);
                candidate.ifPresent(candidates::add);
            }

            log.info("perf operation=download video_id={} total_bytes={} download_ms={} requests={}",
                    videoId, reader.totalBytes(), reader.totalTimeMs(), reader.requestCount());

            if (candidates.isEmpty()) {
                return CandidateThumbnailExtractionResult.failure("No keyframes could be extracted");
            }

            return CandidateThumbnailExtractionResult.success(candidates);

        } catch (Mp4ParseException e) {
            log.error("MP4 parse error for video {}", videoId, e);
            return CandidateThumbnailExtractionResult.failure("MP4 parse error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to extract thumbnails for video {}", videoId, e);
            return CandidateThumbnailExtractionResult.failure(e.getMessage());
        }
    }

    /**
     * moov box를 찾아 다운로드하고 비디오 트랙 정보를 파싱한다.
     */
    private VideoTrackInfo fetchAndParseMetadata(VideoFileReader reader, String fileKey) {
        long fileSize = reader.getFileSize(fileKey);
        byte[] probeData = reader.getRange(fileKey, 0, Math.min(INITIAL_PROBE_SIZE, fileSize));

        Optional<Mp4BoxHeader> moovHeader = Mp4Parser.findMoovBox(probeData);

        byte[] moovData;

        if (moovHeader.isPresent()) {
            Mp4BoxHeader moov = moovHeader.get();
            boolean moovAlreadyInProbeData =
                    moov.offset() < INITIAL_PROBE_SIZE
                    && moov.offset() + moov.totalSize() <= probeData.length;

            if (moovAlreadyInProbeData) {
                moovData = new byte[(int) moov.totalSize()];
                System.arraycopy(probeData, (int) moov.offset(), moovData, 0, moovData.length);
            } else {
                moovData = reader.getRange(fileKey, moov.offset(), moov.totalSize());
            }
        } else {
            // moov가 프로브 데이터에 없으면 mdat 뒤 위치를 추정
            long inferredOffset = Mp4Parser.inferMoovOffsetAfterMdat(probeData, fileSize);

            // 추정 위치에서 실제 moov 헤더 확인
            byte[] moovHeaderBytes = reader.getRange(fileKey, inferredOffset, 16);
            Mp4BoxHeader verifiedHeader = Mp4Parser.readBoxHeader(moovHeaderBytes, 0);

            if (!"moov".equals(verifiedHeader.type())) {
                throw new Mp4ParseException(
                        "Expected moov at offset %d but found '%s'".formatted(
                                inferredOffset, verifiedHeader.type()));
            }

            moovData = reader.getRange(fileKey, inferredOffset, verifiedHeader.totalSize());
        }

        return Mp4Parser.parseVideoTrack(moovData);
    }

    /**
     * 특정 세그먼트에서 최고 sharpness 키프레임을 추출한다.
     */
    private Optional<CandidateFrame> extractBestKeyframe(
            VideoFileReader reader, String fileKey, VideoTrackInfo trackInfo,
            FrameDecoder decoder, TimeRange segment) {

        List<KeyframeInfo> keyframes = trackInfo.findKeyframesInRange(segment.startMs(), segment.endMs());

        if (keyframes.isEmpty()) {
            long midMs = (segment.startMs() + segment.endMs()) / 2;
            KeyframeInfo nearest = trackInfo.findNearestKeyframe(midMs);
            if (nearest == null) return Optional.empty();
            keyframes = List.of(nearest);
            log.warn("No keyframes in [{}-{}ms], using nearest at {}ms",
                    segment.startMs(), segment.endMs(), nearest.timestampMs());
        }

        CandidateFrame best = null;

        for (KeyframeInfo keyframe : keyframes) {
            try {
                // 키 프레임 불러오기
                byte[] sampleData = reader.getRange(fileKey, keyframe.fileOffset(), keyframe.size());

                // 디코딩
                BufferedImage image = decoder.decode(
                        trackInfo.codecConfig(), sampleData, trackInfo.nalLengthSize(),
                        trackInfo.width(), trackInfo.height());

                // sharpness 계산
                double sharpness = sharpnessCalculator.calculate(image);
                log.debug("Keyframe at {}ms: size={}B, sharpness={}", keyframe.timestampMs(), keyframe.size(), sharpness);

                // sharpness가 가장 높은 키 프레임 선택
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
                .orElseThrow(() -> new Mp4ParseException("Unsupported codec: " + codecType));
    }

    /**
     * 영상 길이에 따라 세그먼트를 결정한다 (기존 VideoThumbnailExtractor와 동일한 로직).
     */
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
}
