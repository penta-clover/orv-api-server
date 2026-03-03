package com.orv.worker.thumbnailextraction.service;

import com.orv.archive.repository.VideoFileReader;
import com.orv.archive.service.infrastructure.mp4.Mp4ParseException;
import com.orv.archive.service.infrastructure.mp4.VideoTrackInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Mp4MetadataFetcherTest {

    @Mock
    private VideoFileReader videoFileReader;

    private Mp4MetadataFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new Mp4MetadataFetcher();
    }

    // ── moov가 파일 앞에 있는 경우 ────────────────────────────────────

    @Nested
    @DisplayName("moov가 파일 앞에 있는 경우")
    class MoovAtFrontTest {

        @Test
        @DisplayName("작은 파일이면 비디오 트랙 정보가 정상적으로 파싱된다")
        void fetch_smallFile_returnsVideoTrackInfo() {
            String fileKey = "test-video";
            byte[] fileData = buildTestFile(buildTestMoov());

            stubFileSlice(fileKey, fileData);

            // when
            VideoTrackInfo trackInfo = fetcher.fetch(videoFileReader, fileKey);

            // then
            assertThat(trackInfo.codecType()).isEqualTo("avc1");
            assertThat(trackInfo.width()).isEqualTo(1920);
            assertThat(trackInfo.height()).isEqualTo(1080);
            assertThat(trackInfo.durationMs()).isEqualTo(5000);
        }

        @Test
        @DisplayName("moov가 큰 파일이어도 비디오 트랙 정보가 정상적으로 파싱된다")
        void fetch_largeMoov_returnsVideoTrackInfo() {
            String fileKey = "test-video";
            byte[] largeMoov = buildLargeMoov(4000);
            byte[] ftyp = buildBox("ftyp", ascii("isom"), uint32(0x200));
            byte[] mdat = buildBox("mdat", new byte[100]);
            byte[] fileData = concat(ftyp, largeMoov, mdat);

            stubFileSlice(fileKey, fileData);

            // when
            VideoTrackInfo trackInfo = fetcher.fetch(videoFileReader, fileKey);

            // then
            assertThat(trackInfo.codecType()).isEqualTo("avc1");
            assertThat(trackInfo.durationMs()).isEqualTo(5000);
        }
    }

    // ── moov가 mdat 뒤에 있는 경우 ───────────────────────────────────

    @Nested
    @DisplayName("moov가 mdat 뒤에 있는 경우")
    class MoovAfterMdatTest {

        @Test
        @DisplayName("moov가 mdat 뒤에 있어도 비디오 트랙 정보가 정상적으로 파싱된다")
        void fetch_moovAfterMdat_returnsVideoTrackInfo() {
            String fileKey = "test-video";
            byte[] fileData = buildTestFileReversed(buildTestMoov(), 100_000);

            stubFileSlice(fileKey, fileData);

            // when
            VideoTrackInfo trackInfo = fetcher.fetch(videoFileReader, fileKey);

            // then
            assertThat(trackInfo.codecType()).isEqualTo("avc1");
            assertThat(trackInfo.durationMs()).isEqualTo(5000);
        }

        @Test
        @DisplayName("moov 없이 mdat만 있으면 Mp4ParseException이 발생한다")
        void fetch_noMoovAfterMdat_throwsMp4ParseException() {
            String fileKey = "corrupt-video";
            byte[] ftyp = buildBox("ftyp", ascii("isom"), uint32(0x200));
            byte[] mdat = buildBox("mdat", new byte[100_000]);
            byte[] freeBox = buildBox("free", new byte[100]);
            byte[] fileData = concat(ftyp, mdat, freeBox);

            stubFileSlice(fileKey, fileData);

            // when/then
            assertThatThrownBy(() -> fetcher.fetch(videoFileReader, fileKey))
                    .isInstanceOf(Mp4ParseException.class)
                    .hasMessageContaining("Expected moov");
        }
    }

    // ── 파싱 실패 케이스 ─────────────────────────────────────────────

    @Nested
    @DisplayName("파싱 실패 케이스")
    class ParseFailureTest {

        @Test
        @DisplayName("유효한 MP4 구조가 아니면 Mp4ParseException이 발생한다")
        void fetch_invalidMp4Structure_throwsMp4ParseException() {
            String fileKey = "corrupt-video";

            when(videoFileReader.getFileSize(fileKey)).thenReturn(10000L);
            when(videoFileReader.getRange(eq(fileKey), eq(0L), anyLong()))
                    .thenReturn(new byte[]{0x00, 0x00, 0x00, 0x08, 'f', 't', 'y', 'p'});

            assertThatThrownBy(() -> fetcher.fetch(videoFileReader, fileKey))
                    .isInstanceOf(Mp4ParseException.class);
        }
    }

    // ── 공통 stubbing ────────────────────────────────────────────────

    private void stubFileSlice(String fileKey, byte[] fileData) {
        when(videoFileReader.getFileSize(fileKey)).thenReturn((long) fileData.length);
        when(videoFileReader.getRange(eq(fileKey), anyLong(), anyLong()))
                .thenAnswer(inv -> {
                    int start = ((Long) inv.getArgument(1)).intValue();
                    int length = ((Long) inv.getArgument(2)).intValue();
                    int end = Math.min(start + length, fileData.length);
                    return Arrays.copyOfRange(fileData, start, end);
                });
    }

    // ── MP4 바이트 빌더 ──────────────────────────────────────────────

    private static byte[] buildTestMoov() {
        byte[] sps = {0x67, 0x42, 0x00, 0x1E};
        byte[] pps = {0x68, (byte) 0xCE, 0x38, (byte) 0x80};

        byte[] avcCBox = buildBox("avcC", buildAvcCBytes(sps, pps, 3));

        byte[] visualEntry = new byte[78];
        visualEntry[24] = (byte) (1920 >> 8); visualEntry[25] = (byte) 1920;
        visualEntry[26] = (byte) (1080 >> 8); visualEntry[27] = (byte) 1080;
        visualEntry[6] = 0; visualEntry[7] = 1;

        byte[] stsd = buildBox("stsd", versionFlags(0, 0), uint32(1),
                buildBox("avc1", visualEntry, avcCBox));
        byte[] stts = buildBox("stts", versionFlags(0, 0), uint32(1), uint32(5), uint32(1000));
        byte[] stss = buildBox("stss", versionFlags(0, 0), uint32(2), uint32(1), uint32(3));
        byte[] stsc = buildBox("stsc", versionFlags(0, 0), uint32(1), uint32(1), uint32(5), uint32(1));
        byte[] stsz = buildBox("stsz", versionFlags(0, 0), uint32(0), uint32(5),
                uint32(50000), uint32(10000), uint32(10000), uint32(10000), uint32(10000));
        byte[] stco = buildBox("stco", versionFlags(0, 0), uint32(1), uint32(500));

        byte[] stbl = buildBox("stbl", stsd, stts, stss, stsc, stsz, stco);
        byte[] hdlr = buildBox("hdlr", versionFlags(0, 0),
                uint32(0), ascii("vide"), new byte[12], ascii("VideoHandler\0"));
        byte[] mdhd = buildBox("mdhd", versionFlags(0, 0),
                uint32(0), uint32(0), uint32(1000), uint32(5000), new byte[4]);

        byte[] mdia = buildBox("mdia", mdhd, hdlr, buildBox("minf", stbl));
        return buildBox("moov", buildBox("trak", mdia));
    }

    private static byte[] buildLargeMoov(int extraPaddingSize) {
        byte[] sps = {0x67, 0x42, 0x00, 0x1E};
        byte[] pps = {0x68, (byte) 0xCE, 0x38, (byte) 0x80};

        byte[] avcCBox = buildBox("avcC", buildAvcCBytes(sps, pps, 3));

        byte[] visualEntry = new byte[78];
        visualEntry[24] = (byte) (1920 >> 8); visualEntry[25] = (byte) 1920;
        visualEntry[26] = (byte) (1080 >> 8); visualEntry[27] = (byte) 1080;
        visualEntry[6] = 0; visualEntry[7] = 1;

        byte[] stsd = buildBox("stsd", versionFlags(0, 0), uint32(1),
                buildBox("avc1", visualEntry, avcCBox));
        byte[] stts = buildBox("stts", versionFlags(0, 0), uint32(1), uint32(5), uint32(1000));
        byte[] stss = buildBox("stss", versionFlags(0, 0), uint32(2), uint32(1), uint32(3));
        byte[] stsc = buildBox("stsc", versionFlags(0, 0), uint32(1), uint32(1), uint32(5), uint32(1));
        byte[] stsz = buildBox("stsz", versionFlags(0, 0), uint32(0), uint32(5),
                uint32(50000), uint32(10000), uint32(10000), uint32(10000), uint32(10000));
        byte[] stco = buildBox("stco", versionFlags(0, 0), uint32(1), uint32(500));

        byte[] padding = buildBox("free", new byte[extraPaddingSize]);

        byte[] stbl = buildBox("stbl", stsd, stts, stss, stsc, stsz, stco);
        byte[] hdlr = buildBox("hdlr", versionFlags(0, 0),
                uint32(0), ascii("vide"), new byte[12], ascii("VideoHandler\0"));
        byte[] mdhd = buildBox("mdhd", versionFlags(0, 0),
                uint32(0), uint32(0), uint32(1000), uint32(5000), new byte[4]);

        byte[] mdia = buildBox("mdia", mdhd, hdlr, buildBox("minf", stbl));
        return buildBox("moov", buildBox("trak", mdia), padding);
    }

    private static byte[] buildTestFile(byte[] moovData) {
        byte[] ftyp = buildBox("ftyp", ascii("isom"), uint32(0x200));
        byte[] mdat = buildBox("mdat", new byte[100]);
        return concat(ftyp, moovData, mdat);
    }

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
        return s.getBytes(StandardCharsets.ISO_8859_1);
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
}
