package com.orv.worker.thumbnailextraction.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.orv.archive.domain.CandidateThumbnailExtractionResult;
import com.orv.archive.domain.Video;
import com.orv.archive.repository.VideoFileReader;
import com.orv.archive.repository.VideoRepository;
import com.orv.archive.service.infrastructure.ImageSharpnessCalculator;
import com.orv.archive.service.infrastructure.mp4.Mp4ParseException;
import com.orv.archive.service.infrastructure.mp4.SampleTableInfo;
import com.orv.archive.service.infrastructure.mp4.VideoTrackInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RangeBasedThumbnailExtractorTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private VideoFileReader videoFileReader;

    @Mock
    private Mp4MetadataFetcher mp4MetadataFetcher;

    @Mock
    private ImageSharpnessCalculator sharpnessCalculator;

    @Mock
    private FrameDecoder frameDecoder;

    private RangeBasedThumbnailExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new RangeBasedThumbnailExtractor(
                videoRepository,
                videoFileReader,
                mp4MetadataFetcher,
                List.of(frameDecoder),
                sharpnessCalculator
        );
    }

    // ── extractCandidates 정상 케이스 ────────────────────────────────

    @Nested
    @DisplayName("extractCandidates 정상 케이스")
    class ExtractCandidatesSuccessTest {

        @Test
        @DisplayName("정상적인 비디오에서 키프레임을 디코딩하면 후보 썸네일이 추출된다")
        void extractCandidates_validVideoWithDecodableKeyframes_returnsSuccessWithCandidates() {
            // given
            UUID videoId = UUID.randomUUID();
            String fileKey = "archive/videos/test-video";

            when(videoRepository.findById(videoId)).thenReturn(Optional.of(videoWithFileKey(videoId, fileKey)));
            when(mp4MetadataFetcher.fetch(any(VideoFileReader.class), eq(fileKey)))
                    .thenReturn(buildVideoTrackInfo5s());
            when(videoFileReader.getRange(eq(fileKey), anyLong(), anyLong()))
                    .thenReturn(new byte[]{0x65, 0x00, 0x00, 0x01});
            when(frameDecoder.supports("avc1")).thenReturn(true);
            BufferedImage mockImage = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
            when(frameDecoder.decode(any(), any(), anyInt(), anyInt(), anyInt())).thenReturn(mockImage);
            when(sharpnessCalculator.calculate(any())).thenReturn(100.0);

            // when
            CandidateThumbnailExtractionResult result = extractor.extractCandidates(videoId);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.candidates()).isNotEmpty();
        }

    }

    // ── extractCandidates 실패 케이스 ────────────────────────────────

    @Nested
    @DisplayName("extractCandidates 실패 케이스")
    class ExtractCandidatesFailureTest {

        @Test
        @DisplayName("비디오를 찾을 수 없으면 실패 결과를 반환한다")
        void extractCandidates_videoNotFound_returnsFailure() {
            UUID videoId = UUID.randomUUID();
            when(videoRepository.findById(videoId)).thenReturn(Optional.empty());

            CandidateThumbnailExtractionResult result = extractor.extractCandidates(videoId);

            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).contains("Video file key not found");
        }

        @Test
        @DisplayName("MP4 파싱에 실패하면 실패 결과가 반환된다")
        void extractCandidates_mp4ParseException_returnsFailureWithErrorLog() {
            // given
            UUID videoId = UUID.randomUUID();
            String fileKey = "archive/videos/corrupt-video";

            when(videoRepository.findById(videoId)).thenReturn(Optional.of(videoWithFileKey(videoId, fileKey)));
            when(mp4MetadataFetcher.fetch(any(VideoFileReader.class), eq(fileKey)))
                    .thenThrow(new Mp4ParseException("Cannot locate mdat box"));

            Logger logger = (Logger) LoggerFactory.getLogger(RangeBasedThumbnailExtractor.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);

            try {
                // when
                CandidateThumbnailExtractionResult result = extractor.extractCandidates(videoId);

                // then
                assertThat(result.success()).isFalse();
                assertThat(result.errorMessage()).contains("MP4 parse error");

                assertThat(appender.list)
                        .anyMatch(event ->
                                event.getLevel() == Level.ERROR
                                        && event.getFormattedMessage().contains("MP4 parse error"));
            } finally {
                logger.detachAppender(appender);
            }
        }

        @Test
        @DisplayName("지원하지 않는 코덱이면 실패 결과가 반환된다")
        void extractCandidates_unsupportedCodec_returnsFailureWithErrorLog() {
            // given
            UUID videoId = UUID.randomUUID();
            String fileKey = "archive/videos/hevc-video";

            when(videoRepository.findById(videoId)).thenReturn(Optional.of(videoWithFileKey(videoId, fileKey)));
            when(mp4MetadataFetcher.fetch(any(VideoFileReader.class), eq(fileKey)))
                    .thenReturn(buildVideoTrackInfo5s());
            when(frameDecoder.supports(anyString())).thenReturn(false);

            Logger logger = (Logger) LoggerFactory.getLogger(RangeBasedThumbnailExtractor.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);

            try {
                // when
                CandidateThumbnailExtractionResult result = extractor.extractCandidates(videoId);

                // then
                assertThat(result.success()).isFalse();
                assertThat(result.errorMessage()).contains("Unsupported codec");

                assertThat(appender.list)
                        .anyMatch(event -> event.getLevel() == Level.ERROR);
            } finally {
                logger.detachAppender(appender);
            }
        }

        @Test
        @DisplayName("일부 키프레임 디코딩에 실패하면 해당 프레임만 건너뛰고 나머지로 성공한다")
        void extractCandidates_someKeyframeDecodingFails_skipsFailedAndReturnsSuccess() {
            // given
            UUID videoId = UUID.randomUUID();
            String fileKey = "archive/videos/test-video";

            when(videoRepository.findById(videoId)).thenReturn(Optional.of(videoWithFileKey(videoId, fileKey)));
            when(mp4MetadataFetcher.fetch(any(VideoFileReader.class), eq(fileKey)))
                    .thenReturn(buildVideoTrackInfo5s());
            when(videoFileReader.getRange(eq(fileKey), anyLong(), anyLong()))
                    .thenReturn(new byte[]{0x65, 0x00});
            when(frameDecoder.supports("avc1")).thenReturn(true);

            // 첫 번째 호출은 실패, 이후 호출은 성공
            BufferedImage mockImage = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
            when(frameDecoder.decode(any(), any(), anyInt(), anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("Decode failed"))
                    .thenReturn(mockImage);
            when(sharpnessCalculator.calculate(any())).thenReturn(50.0);

            Logger logger = (Logger) LoggerFactory.getLogger(RangeBasedThumbnailExtractor.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);

            try {
                // when
                CandidateThumbnailExtractionResult result = extractor.extractCandidates(videoId);

                assertThat(result.success()).isTrue();
                assertThat(result.candidates()).isNotEmpty();
                assertThat(appender.list)
                        .anyMatch(event ->
                                event.getLevel() == Level.WARN
                                        && event.getFormattedMessage().contains("Failed to decode keyframe"));
            } finally {
                logger.detachAppender(appender);
            }
        }

        @Test
        @DisplayName("모든 키프레임 디코딩에 실패하면 실패 결과를 반환한다")
        void extractCandidates_allKeyframeDecodingFails_returnsFailure() {
            // given
            UUID videoId = UUID.randomUUID();
            String fileKey = "archive/videos/test-video";

            when(videoRepository.findById(videoId)).thenReturn(Optional.of(videoWithFileKey(videoId, fileKey)));
            when(mp4MetadataFetcher.fetch(any(VideoFileReader.class), eq(fileKey)))
                    .thenReturn(buildVideoTrackInfo5s());
            when(videoFileReader.getRange(eq(fileKey), anyLong(), anyLong()))
                    .thenReturn(new byte[]{0x65, 0x00});
            when(frameDecoder.supports("avc1")).thenReturn(true);
            when(frameDecoder.decode(any(), any(), anyInt(), anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("Decode failed"));

            // when
            CandidateThumbnailExtractionResult result = extractor.extractCandidates(videoId);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).contains("No keyframes could be extracted");
        }
    }

    // ── computeSegments ──────────────────────────────────────────────

    @Nested
    @DisplayName("computeSegments")
    class ComputeSegmentsTest {

        @Test
        @DisplayName("5초 이하 영상이면 세그먼트가 1개 생성된다")
        void computeSegments_durationUnder5Seconds_returnsOneSegment() throws Exception {
            List<?> segments = invokeComputeSegments(3000);
            assertThat(segments).hasSize(1);
        }

        @Test
        @DisplayName("5초 초과 15초 미만 영상이면 세그먼트가 2개 생성된다")
        void computeSegments_durationBetween5And15Seconds_returnsTwoSegments() throws Exception {
            List<?> segments = invokeComputeSegments(10000);
            assertThat(segments).hasSize(2);
        }

        @Test
        @DisplayName("15초 이상 영상이면 세그먼트가 3개 생성된다")
        void computeSegments_durationOver15Seconds_returnsThreeSegments() throws Exception {
            List<?> segments = invokeComputeSegments(30000);
            assertThat(segments).hasSize(3);
        }

        @Test
        @DisplayName("정확히 5초 영상이면 세그먼트가 1개 생성된다")
        void computeSegments_durationExactly5Seconds_returnsOneSegment() throws Exception {
            List<?> segments = invokeComputeSegments(5000);
            assertThat(segments).hasSize(1);
        }

        private List<?> invokeComputeSegments(long durationMs) throws Exception {
            Method method = RangeBasedThumbnailExtractor.class.getDeclaredMethod("computeSegments", long.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<?> result = (List<?>) method.invoke(extractor, durationMs);
            return result;
        }
    }

    // ── VideoTrackInfo 빌더 ──────────────────────────────────────────

    private static VideoTrackInfo buildVideoTrackInfo5s() {
        SampleTableInfo sampleTable = new SampleTableInfo(
                List.of(new SampleTableInfo.SttsEntry(5, 1000)),
                new int[]{1, 3},  // sync samples (1-based)
                List.of(new SampleTableInfo.StscEntry(1, 5, 1)),
                new int[]{50000, 10000, 10000, 10000, 10000},
                new long[]{500}
        );
        byte[] dummyCodecConfig = {1, 0x42, 0x00, 0x1E, (byte) 0xFF};
        return new VideoTrackInfo(1000, 5000, 1920, 1080, "avc1", dummyCodecConfig, 4, sampleTable);
    }

    private static Video videoWithFileKey(UUID videoId, String fileKey) {
        Video video = new Video();
        video.setId(videoId);
        video.setVideoFileKey(fileKey);
        return video;
    }
}
