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
    private ImageSharpnessCalculator sharpnessCalculator;

    @Mock
    private FrameDecoder frameDecoder;

    private RangeBasedThumbnailExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new RangeBasedThumbnailExtractor(
                videoRepository,
                videoFileReader,
                List.of(frameDecoder),
                sharpnessCalculator
        );
    }

    // ── extractCandidates 정상 케이스 ────────────────────────────────

    @Nested
    @DisplayName("extractCandidates 정상 케이스")
    class ExtractCandidatesSuccessTest {

        @Test
        @DisplayName("전체 플로우 성공: fileKey 조회 → 메타데이터 파싱 → 디코딩 → sharpness 계산")
        void fullFlowSuccess() {
            // given
            UUID videoId = UUID.randomUUID();
            String fileKey = "archive/videos/test-video";
            byte[] moovData = buildTestMoov();
            byte[] fileData = buildTestFile(moovData);

            when(videoRepository.findById(videoId)).thenReturn(Optional.of(videoWithFileKey(videoId, fileKey)));
            when(videoFileReader.getFileSize(fileKey)).thenReturn((long) fileData.length);
            when(videoFileReader.getRange(eq(fileKey), anyLong(), anyLong()))
                    .thenAnswer(invocation -> {
                        long offset = invocation.getArgument(1);
                        long length = invocation.getArgument(2);
                        if (offset == 0L) {
                            return java.util.Arrays.copyOfRange(fileData, 0, (int) Math.min(length, fileData.length));
                        }
                        return new byte[]{0x65, 0x00, 0x00, 0x01};
                    });

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

        @Test
        @DisplayName("moov가 mdat 뒤에 있을 때 전체 플로우 성공")
        void moovAfterMdat() {
            // given: ftyp + mdat(100KB) + moov 순서
            UUID videoId = UUID.randomUUID();
            String fileKey = "archive/videos/test-video";
            byte[] moovData = buildTestMoov();
            byte[] fileData = buildTestFileReversed(moovData, 100000);

            when(videoRepository.findById(videoId)).thenReturn(Optional.of(videoWithFileKey(videoId, fileKey)));
            when(videoFileReader.getFileSize(fileKey)).thenReturn((long) fileData.length);
            // 모든 Range GET을 파일 데이터 슬라이스로 응답
            when(videoFileReader.getRange(eq(fileKey), anyLong(), anyLong()))
                    .thenAnswer(invocation -> {
                        long offset = invocation.getArgument(1);
                        long length = invocation.getArgument(2);
                        int start = (int) offset;
                        int end = (int) Math.min(start + length, fileData.length);
                        return java.util.Arrays.copyOfRange(fileData, start, end);
                    });

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

        @Test
        @DisplayName("세그먼트 내 키프레임 없을 때 nearest 키프레임 폴백 + warn 로그")
        void nearestKeyframeFallback() {
            // given: 10초 영상, 키프레임 1개(0ms) → 세그먼트 [5000,10000]에 키프레임 없음
            UUID videoId = UUID.randomUUID();
            String fileKey = "archive/videos/test-video";
            byte[] moovData = buildTestMoovLong();
            byte[] fileData = buildTestFile(moovData);

            when(videoRepository.findById(videoId)).thenReturn(Optional.of(videoWithFileKey(videoId, fileKey)));
            when(videoFileReader.getFileSize(fileKey)).thenReturn((long) fileData.length);
            when(videoFileReader.getRange(eq(fileKey), anyLong(), anyLong()))
                    .thenAnswer(invocation -> {
                        long offset = invocation.getArgument(1);
                        long length = invocation.getArgument(2);
                        if (offset == 0L) {
                            return java.util.Arrays.copyOfRange(fileData, 0, (int) Math.min(length, fileData.length));
                        }
                        return new byte[]{0x65, 0x00, 0x00, 0x01};
                    });

            when(frameDecoder.supports("avc1")).thenReturn(true);
            BufferedImage mockImage = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
            when(frameDecoder.decode(any(), any(), anyInt(), anyInt(), anyInt())).thenReturn(mockImage);
            when(sharpnessCalculator.calculate(any())).thenReturn(100.0);

            // 로그 캡처
            Logger logger = (Logger) LoggerFactory.getLogger(RangeBasedThumbnailExtractor.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);

            try {
                // when
                CandidateThumbnailExtractionResult result = extractor.extractCandidates(videoId);

                // then: 2 세그먼트 모두에서 후보 추출 성공
                assertThat(result.success()).isTrue();
                assertThat(result.candidates()).hasSize(2);

                // warn 로그: "No keyframes in" 메시지 확인 (두 번째 세그먼트에서 발생)
                assertThat(appender.list)
                        .anyMatch(event ->
                                event.getLevel() == Level.WARN
                                        && event.getFormattedMessage().contains("No keyframes in"));
            } finally {
                logger.detachAppender(appender);
            }
        }
    }

    // ── extractCandidates 실패 케이스 ────────────────────────────────

    @Nested
    @DisplayName("extractCandidates 실패 케이스")
    class ExtractCandidatesFailureTest {

        @Test
        @DisplayName("파일 키가 없으면 failure 반환")
        void fileKeyNotFound() {
            UUID videoId = UUID.randomUUID();
            when(videoRepository.findById(videoId)).thenReturn(Optional.empty());

            CandidateThumbnailExtractionResult result = extractor.extractCandidates(videoId);

            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).contains("Video file key not found");
        }

        @Test
        @DisplayName("MP4 파싱 오류 시 failure 반환 + error 로그 발생")
        void mp4ParseError() {
            // given
            UUID videoId = UUID.randomUUID();
            String fileKey = "archive/videos/corrupt-video";

            when(videoRepository.findById(videoId)).thenReturn(Optional.of(videoWithFileKey(videoId, fileKey)));
            when(videoFileReader.getFileSize(fileKey)).thenReturn(10000L);
            // 잘못된 데이터 반환 → Mp4ParseException 발생
            when(videoFileReader.getRange(eq(fileKey), eq(0L), anyLong()))
                    .thenReturn(new byte[]{0x00, 0x00, 0x00, 0x08, 'f', 't', 'y', 'p'});

            // 로그 캡처
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

                // error 로그 발생 확인
                assertThat(appender.list)
                        .anyMatch(event ->
                                event.getLevel() == Level.ERROR
                                        && event.getFormattedMessage().contains("MP4 parse error"));
            } finally {
                logger.detachAppender(appender);
            }
        }

        @Test
        @DisplayName("미지원 코덱 시 failure 반환 + error 로그 발생")
        void unsupportedCodec() {
            // given
            UUID videoId = UUID.randomUUID();
            String fileKey = "archive/videos/hevc-video";
            byte[] moovData = buildTestMoov();
            byte[] fileData = buildTestFile(moovData);

            when(videoRepository.findById(videoId)).thenReturn(Optional.of(videoWithFileKey(videoId, fileKey)));
            when(videoFileReader.getFileSize(fileKey)).thenReturn((long) fileData.length);
            when(videoFileReader.getRange(eq(fileKey), eq(0L), anyLong()))
                    .thenReturn(fileData);
            // 디코더가 지원하지 않음
            when(frameDecoder.supports(anyString())).thenReturn(false);

            // 로그 캡처
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
        @DisplayName("개별 키프레임 디코딩 실패 시 warn 로그 + 해당 프레임만 스킵")
        void decodingFailureSkipsFrame() {
            // given
            UUID videoId = UUID.randomUUID();
            String fileKey = "archive/videos/test-video";
            byte[] moovData = buildTestMoov();
            byte[] fileData = buildTestFile(moovData);

            when(videoRepository.findById(videoId)).thenReturn(Optional.of(videoWithFileKey(videoId, fileKey)));
            when(videoFileReader.getFileSize(fileKey)).thenReturn((long) fileData.length);
            when(videoFileReader.getRange(eq(fileKey), anyLong(), anyLong()))
                    .thenAnswer(invocation -> {
                        long offset = invocation.getArgument(1);
                        long length = invocation.getArgument(2);
                        if (offset == 0L) {
                            return java.util.Arrays.copyOfRange(fileData, 0, (int) Math.min(length, fileData.length));
                        }
                        return new byte[]{0x65, 0x00};
                    });

            when(frameDecoder.supports("avc1")).thenReturn(true);

            // 첫 번째 호출은 실패, 이후 호출은 성공
            BufferedImage mockImage = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
            when(frameDecoder.decode(any(), any(), anyInt(), anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("Decode failed"))
                    .thenReturn(mockImage);
            when(sharpnessCalculator.calculate(any())).thenReturn(50.0);

            // 로그 캡처
            Logger logger = (Logger) LoggerFactory.getLogger(RangeBasedThumbnailExtractor.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);

            try {
                // when
                CandidateThumbnailExtractionResult result = extractor.extractCandidates(videoId);

                // then: 성공 (일부 키프레임은 디코딩 성공)
                assertThat(result.success()).isTrue();
                assertThat(result.candidates()).isNotEmpty();

                // warn 로그 발생 확인
                assertThat(appender.list)
                        .anyMatch(event ->
                                event.getLevel() == Level.WARN
                                        && event.getFormattedMessage().contains("Failed to decode keyframe"));
            } finally {
                logger.detachAppender(appender);
            }
        }

        @Test
        @DisplayName("모든 키프레임 디코딩 실패 시 failure 반환")
        void allDecodingsFail() {
            // given
            UUID videoId = UUID.randomUUID();
            String fileKey = "archive/videos/test-video";
            byte[] moovData = buildTestMoov();
            byte[] fileData = buildTestFile(moovData);

            when(videoRepository.findById(videoId)).thenReturn(Optional.of(videoWithFileKey(videoId, fileKey)));
            when(videoFileReader.getFileSize(fileKey)).thenReturn((long) fileData.length);
            when(videoFileReader.getRange(eq(fileKey), anyLong(), anyLong()))
                    .thenAnswer(invocation -> {
                        long offset = invocation.getArgument(1);
                        long length = invocation.getArgument(2);
                        if (offset == 0L) {
                            return java.util.Arrays.copyOfRange(fileData, 0, (int) Math.min(length, fileData.length));
                        }
                        return new byte[]{0x65, 0x00};
                    });

            when(frameDecoder.supports("avc1")).thenReturn(true);
            when(frameDecoder.decode(any(), any(), anyInt(), anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("Decode failed"));

            // when
            CandidateThumbnailExtractionResult result = extractor.extractCandidates(videoId);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).contains("No keyframes could be extracted");
        }

        @Test
        @DisplayName("moov 예상 위치에 다른 box가 있으면 failure 반환 + error 로그")
        void moovAfterMdatWrongBoxType() {
            // given: ftyp + mdat + free (moov 대신 free box)
            UUID videoId = UUID.randomUUID();
            String fileKey = "archive/videos/corrupt-video";

            byte[] ftyp = buildBox("ftyp", ascii("isom"), uint32(0x200));
            byte[] mdat = buildBox("mdat", new byte[100000]);
            byte[] freeBox = buildBox("free", new byte[100]);
            byte[] fileData = concat(ftyp, mdat, freeBox);

            when(videoRepository.findById(videoId)).thenReturn(Optional.of(videoWithFileKey(videoId, fileKey)));
            when(videoFileReader.getFileSize(fileKey)).thenReturn((long) fileData.length);
            when(videoFileReader.getRange(eq(fileKey), anyLong(), anyLong()))
                    .thenAnswer(invocation -> {
                        long offset = invocation.getArgument(1);
                        long length = invocation.getArgument(2);
                        int start = (int) offset;
                        int end = (int) Math.min(start + length, fileData.length);
                        return java.util.Arrays.copyOfRange(fileData, start, end);
                    });

            // 로그 캡처
            Logger logger = (Logger) LoggerFactory.getLogger(RangeBasedThumbnailExtractor.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);

            try {
                // when
                CandidateThumbnailExtractionResult result = extractor.extractCandidates(videoId);

                // then
                assertThat(result.success()).isFalse();
                assertThat(result.errorMessage()).contains("Expected moov");

                assertThat(appender.list)
                        .anyMatch(event -> event.getLevel() == Level.ERROR);
            } finally {
                logger.detachAppender(appender);
            }
        }
    }

    // ── computeSegments ──────────────────────────────────────────────

    @Nested
    @DisplayName("computeSegments")
    class ComputeSegmentsTest {

        @Test
        @DisplayName("5초 이하 영상 → 세그먼트 1개")
        void shortVideo() throws Exception {
            List<?> segments = invokeComputeSegments(3000);
            assertThat(segments).hasSize(1);
        }

        @Test
        @DisplayName("5~15초 영상 → 세그먼트 2개")
        void mediumVideo() throws Exception {
            List<?> segments = invokeComputeSegments(10000);
            assertThat(segments).hasSize(2);
        }

        @Test
        @DisplayName("15초 이상 영상 → 세그먼트 3개")
        void longVideo() throws Exception {
            List<?> segments = invokeComputeSegments(30000);
            assertThat(segments).hasSize(3);
        }

        @Test
        @DisplayName("정확히 5초 영상 → 세그먼트 1개 [0, 5000]")
        void exactlyFiveSeconds() throws Exception {
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

    // ── 테스트용 MP4 데이터 빌더 ─────────────────────────────────────

    /**
     * 간단한 테스트용 moov 바이트 생성 (Mp4TestHelper 패턴과 동일한 구조)
     * 5초 영상: timescale=1000, duration=5000, 5 samples, 2 keyframes (sample 0, 2)
     */
    private static byte[] buildTestMoov() {
        byte[] sps = {0x67, 0x42, 0x00, 0x1E};
        byte[] pps = {0x68, (byte) 0xCE, 0x38, (byte) 0x80};

        byte[] avcC = buildAvcCBytes(sps, pps, 3);
        byte[] avcCBox = buildBox("avcC", avcC);

        // VisualSampleEntry (78 bytes fixed fields)
        byte[] visualEntry = new byte[78];
        // width at offset 24-25, height at offset 26-27
        visualEntry[24] = (byte) (1920 >> 8); visualEntry[25] = (byte) 1920;
        visualEntry[26] = (byte) (1080 >> 8); visualEntry[27] = (byte) 1080;
        visualEntry[6] = 0; visualEntry[7] = 1; // data_ref_index = 1

        byte[] avc1Box = buildBox("avc1", visualEntry, avcCBox);
        byte[] stsdContent = concat(versionFlags(0, 0), uint32(1), avc1Box);
        byte[] stsd = buildBox("stsd", stsdContent);

        // stts: 5 samples, delta=1000 (=1s per sample at timescale=1000, total 5s)
        byte[] stts = buildBox("stts", versionFlags(0, 0),
                uint32(1), uint32(5), uint32(1000));

        // stss: sync samples 1, 3 (1-based) → 0-based: 0, 2
        byte[] stss = buildBox("stss", versionFlags(0, 0),
                uint32(2), uint32(1), uint32(3));

        // stsc: 1 entry → chunk 1, 5 samples per chunk
        byte[] stsc = buildBox("stsc", versionFlags(0, 0),
                uint32(1), uint32(1), uint32(5), uint32(1));

        // stsz: 5 samples
        byte[] stsz = buildBox("stsz", versionFlags(0, 0),
                uint32(0), uint32(5), uint32(50000), uint32(10000), uint32(10000), uint32(10000), uint32(10000));

        // stco: 1 chunk at offset 500 (arbitrary file offset within mdat)
        byte[] stco = buildBox("stco", versionFlags(0, 0),
                uint32(1), uint32(500));

        byte[] stbl = buildBox("stbl", stsd, stts, stss, stsc, stsz, stco);
        byte[] minf = buildBox("minf", stbl);

        // hdlr: handler_type = "vide"
        byte[] hdlr = buildBox("hdlr", versionFlags(0, 0),
                uint32(0), ascii("vide"), new byte[12], ascii("VideoHandler\0"));

        // mdhd v0: timescale=1000, duration=5000
        byte[] mdhd = buildBox("mdhd", versionFlags(0, 0),
                uint32(0), uint32(0), uint32(1000), uint32(5000), new byte[4]);

        byte[] mdia = buildBox("mdia", mdhd, hdlr, minf);
        byte[] trak = buildBox("trak", mdia);
        return buildBox("moov", trak);
    }

    /**
     * 10초 영상, 키프레임 1개만 (sample 0).
     * computeSegments(10000) → [0,5000] + [5000,10000]. 두 번째 세그먼트에 키프레임 없음.
     */
    private static byte[] buildTestMoovLong() {
        byte[] sps = {0x67, 0x42, 0x00, 0x1E};
        byte[] pps = {0x68, (byte) 0xCE, 0x38, (byte) 0x80};

        byte[] avcC = buildAvcCBytes(sps, pps, 3);
        byte[] avcCBox = buildBox("avcC", avcC);

        byte[] visualEntry = new byte[78];
        visualEntry[24] = (byte) (1920 >> 8); visualEntry[25] = (byte) 1920;
        visualEntry[26] = (byte) (1080 >> 8); visualEntry[27] = (byte) 1080;
        visualEntry[6] = 0; visualEntry[7] = 1;

        byte[] avc1Box = buildBox("avc1", visualEntry, avcCBox);
        byte[] stsdContent = concat(versionFlags(0, 0), uint32(1), avc1Box);
        byte[] stsd = buildBox("stsd", stsdContent);

        // stts: 10 samples, delta=1000 (=1s per sample, 10s total)
        byte[] stts = buildBox("stts", versionFlags(0, 0),
                uint32(1), uint32(10), uint32(1000));

        // stss: 키프레임 1개만 (sample 1, 1-based)
        byte[] stss = buildBox("stss", versionFlags(0, 0),
                uint32(1), uint32(1));

        // stsc: 1 chunk, 10 samples
        byte[] stsc = buildBox("stsc", versionFlags(0, 0),
                uint32(1), uint32(1), uint32(10), uint32(1));

        // stsz: 10 samples, 각 10000 bytes
        byte[] stsz = buildBox("stsz", versionFlags(0, 0),
                uint32(0), uint32(10),
                uint32(10000), uint32(10000), uint32(10000), uint32(10000), uint32(10000),
                uint32(10000), uint32(10000), uint32(10000), uint32(10000), uint32(10000));

        // stco: 1 chunk at offset 500
        byte[] stco = buildBox("stco", versionFlags(0, 0),
                uint32(1), uint32(500));

        byte[] stbl = buildBox("stbl", stsd, stts, stss, stsc, stsz, stco);
        byte[] minf = buildBox("minf", stbl);

        byte[] hdlr = buildBox("hdlr", versionFlags(0, 0),
                uint32(0), ascii("vide"), new byte[12], ascii("VideoHandler\0"));

        // mdhd v0: timescale=1000, duration=10000 (10초)
        byte[] mdhd = buildBox("mdhd", versionFlags(0, 0),
                uint32(0), uint32(0), uint32(1000), uint32(10000), new byte[4]);

        byte[] mdia = buildBox("mdia", mdhd, hdlr, minf);
        byte[] trak = buildBox("trak", mdia);
        return buildBox("moov", trak);
    }

    /**
     * ftyp + moov + mdat 구조의 파일 바이트 생성
     */
    private static byte[] buildTestFile(byte[] moovData) {
        byte[] ftyp = buildBox("ftyp", ascii("isom"), uint32(0x200));
        byte[] mdat = buildBox("mdat", new byte[100000]); // 100KB dummy mdat
        return concat(ftyp, moovData, mdat);
    }

    /**
     * ftyp + mdat + moov 구조의 파일 바이트 생성 (moov가 mdat 뒤에 오는 경우)
     */
    private static byte[] buildTestFileReversed(byte[] moovData, int mdatContentSize) {
        byte[] ftyp = buildBox("ftyp", ascii("isom"), uint32(0x200));
        byte[] mdat = buildBox("mdat", new byte[mdatContentSize]);
        return concat(ftyp, mdat, moovData);
    }

    // ── 바이트 유틸리티 ──────────────────────────────────────────────

    private static byte[] uint32(long value) {
        return new byte[]{
                (byte) (value >> 24), (byte) (value >> 16),
                (byte) (value >> 8), (byte) value
        };
    }

    private static byte[] versionFlags(int version, int flags) {
        return new byte[]{(byte) version, (byte) (flags >> 16), (byte) (flags >> 8), (byte) flags};
    }

    private static byte[] ascii(String s) {
        return s.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    private static byte[] buildBox(String type, byte[]... children) {
        byte[] content = concat(children);
        int totalSize = 8 + content.length;
        return concat(uint32(totalSize), ascii(type), content);
    }

    private static byte[] buildAvcCBytes(byte[] sps, byte[] pps, int nalLengthSizeMinusOne) {
        return concat(
                new byte[]{1, sps.length > 1 ? sps[1] : 0x42,
                        sps.length > 2 ? sps[2] : 0x00,
                        sps.length > 3 ? sps[3] : 0x1E,
                        (byte) (0xFC | nalLengthSizeMinusOne)},
                new byte[]{(byte) (0xE0 | 1)},
                new byte[]{(byte) (sps.length >> 8), (byte) sps.length}, sps,
                new byte[]{1},
                new byte[]{(byte) (pps.length >> 8), (byte) pps.length}, pps
        );
    }

    private static byte[] concat(byte[]... arrays) {
        int len = 0;
        for (byte[] a : arrays) len += a.length;
        byte[] result = new byte[len];
        int pos = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, pos, a.length);
            pos += a.length;
        }
        return result;
    }

    private static Video videoWithFileKey(UUID videoId, String fileKey) {
        Video video = new Video();
        video.setId(videoId);
        video.setVideoFileKey(fileKey);
        return video;
    }
}
