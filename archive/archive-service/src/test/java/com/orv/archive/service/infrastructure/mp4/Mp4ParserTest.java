package com.orv.archive.service.infrastructure.mp4;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.orv.archive.service.infrastructure.mp4.Mp4TestHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Mp4ParserTest {

    // ── readBoxHeader ────────────────────────────────────────────────

    @Nested
    @DisplayName("readBoxHeader")
    class ReadBoxHeaderTest {

        @Test
        @DisplayName("일반 8바이트 헤더를 읽으면 type, offset, totalSize, headerSize가 정확히 추출된다")
        void readBoxHeader_normalSizeBox_extractsAllFields() {
            // given: size=100, type="moov"
            byte[] data = concat(uint32(100), ascii("moov"), zeros(92));

            // when
            Mp4BoxHeader header = Mp4Parser.readBoxHeader(data, 0);

            // then
            assertThat(header.type()).isEqualTo("moov");
            assertThat(header.offset()).isEqualTo(0);
            assertThat(header.totalSize()).isEqualTo(100);
            assertThat(header.headerSize()).isEqualTo(8);
            assertThat(header.dataOffset()).isEqualTo(8);
            assertThat(header.dataSize()).isEqualTo(92);
        }

        @Test
        @DisplayName("size가 1이면 16바이트 extended size 헤더로 파싱된다")
        void readBoxHeader_extendedSize_parsesAs16ByteHeader() {
            // given: size field=1 → 64-bit extended size follows
            long extendedSize = 0x1_0000_0100L; // 4GB + 256
            byte[] data = concat(
                    uint32(1), ascii("mdat"), int64(extendedSize),
                    zeros(100) // 일부 데이터
            );

            // when
            Mp4BoxHeader header = Mp4Parser.readBoxHeader(data, 0);

            // then
            assertThat(header.type()).isEqualTo("mdat");
            assertThat(header.totalSize()).isEqualTo(extendedSize);
            assertThat(header.headerSize()).isEqualTo(16);
            assertThat(header.dataOffset()).isEqualTo(16);
        }

        @Test
        @DisplayName("지정된 offset에서 box 헤더를 정확히 읽는다")
        void readBoxHeader_nonZeroOffset_readsFromSpecifiedPosition() {
            // given: 50바이트 패딩 후 box
            byte[] padding = zeros(50);
            byte[] box = concat(uint32(32), ascii("free"), zeros(24));
            byte[] data = concat(padding, box);

            // when
            Mp4BoxHeader header = Mp4Parser.readBoxHeader(data, 50);

            // then
            assertThat(header.type()).isEqualTo("free");
            assertThat(header.offset()).isEqualTo(50);
            assertThat(header.totalSize()).isEqualTo(32);
        }

        @Test
        @DisplayName("데이터가 8바이트 미만이면 Mp4ParseException이 발생한다")
        void readBoxHeader_dataLessThan8Bytes_throwsMp4ParseException() {
            byte[] data = new byte[]{0x00, 0x00, 0x00};

            assertThatThrownBy(() -> Mp4Parser.readBoxHeader(data, 0))
                    .isInstanceOf(Mp4ParseException.class)
                    .hasMessageContaining("Not enough data");
        }

        @Test
        @DisplayName("offset이 데이터 끝을 넘어서면 Mp4ParseException이 발생한다")
        void readBoxHeader_offsetBeyondDataEnd_throwsMp4ParseException() {
            byte[] data = new byte[16];

            assertThatThrownBy(() -> Mp4Parser.readBoxHeader(data, 12))
                    .isInstanceOf(Mp4ParseException.class);
        }
    }

    // ── scanTopLevelBoxes ────────────────────────────────────────────

    @Nested
    @DisplayName("scanTopLevelBoxes")
    class ScanTopLevelBoxesTest {

        @Test
        @DisplayName("ftyp, moov, mdat 순서의 top-level box들이 정확히 스캔된다")
        void scanTopLevelBoxes_ftypMoovMdat_returnsThreeBoxesInOrder() {
            // given
            byte[] ftyp = buildFtyp();
            byte[] moov = buildBox("moov", zeros(100));
            byte[] mdat = buildMdat(500);
            byte[] data = concat(ftyp, moov, mdat);

            // when
            List<Mp4BoxHeader> boxes = Mp4Parser.scanTopLevelBoxes(data);

            // then
            assertThat(boxes).hasSize(3);
            assertThat(boxes.get(0).type()).isEqualTo("ftyp");
            assertThat(boxes.get(1).type()).isEqualTo("moov");
            assertThat(boxes.get(2).type()).isEqualTo("mdat");

            assertThat(boxes.get(0).offset()).isEqualTo(0);
            assertThat(boxes.get(1).offset()).isEqualTo(ftyp.length);
            assertThat(boxes.get(2).offset()).isEqualTo(ftyp.length + moov.length);
        }

        @Test
        @DisplayName("빈 데이터를 스캔하면 빈 리스트가 반환된다")
        void scanTopLevelBoxes_emptyData_returnsEmptyList() {
            List<Mp4BoxHeader> boxes = Mp4Parser.scanTopLevelBoxes(new byte[0]);
            assertThat(boxes).isEmpty();
        }
    }

    // ── findMoovBox ──────────────────────────────────────────────────

    @Nested
    @DisplayName("findMoovBox")
    class FindMoovBoxTest {

        @Test
        @DisplayName("moov가 파일 앞에 있으면 정확한 헤더 정보가 반환된다")
        void findMoovBox_moovAfterFtyp_returnsCorrectHeader() {
            // given: ftyp → moov → mdat
            byte[] ftyp = buildFtyp();
            byte[] moov = buildBox("moov", zeros(200));
            byte[] mdat = buildMdat(1000);
            byte[] data = concat(ftyp, moov, mdat);

            // when
            Optional<Mp4BoxHeader> result = Mp4Parser.findMoovBox(data);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().type()).isEqualTo("moov");
            assertThat(result.get().offset()).isEqualTo(ftyp.length);
            assertThat(result.get().totalSize()).isEqualTo(moov.length);
            assertThat(result.get().headerSize()).isEqualTo(8);
        }

        @Test
        @DisplayName("moov 없이 ftyp+mdat만 있으면 empty가 반환된다")
        void findMoovBox_noMoovInProbeData_returnsEmpty() {
            // given: probe에 ftyp + mdat 헤더만 있고, moov는 mdat 뒤에 위치
            byte[] ftyp = buildFtyp();
            long mdatTotalSize = 8 + 100000;
            byte[] mdatHeader = concat(uint32(mdatTotalSize), ascii("mdat"));
            byte[] probeData = concat(ftyp, mdatHeader);

            // when
            Optional<Mp4BoxHeader> result = Mp4Parser.findMoovBox(probeData);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("mdat가 있으면 mdat 직후 오프셋이 반환된다")
        void inferMoovOffsetAfterMdat_mdatInProbeData_returnsOffsetAfterMdat() {
            // given
            byte[] ftyp = buildFtyp();
            long mdatTotalSize = 8 + 100000;
            byte[] mdatHeader = concat(uint32(mdatTotalSize), ascii("mdat"));
            byte[] probeData = concat(ftyp, mdatHeader);

            long expectedMoovOffset = ftyp.length + mdatTotalSize;
            long fileSize = expectedMoovOffset + 500;

            // when
            long inferredOffset = Mp4Parser.inferMoovOffsetAfterMdat(probeData, fileSize);

            // then
            assertThat(inferredOffset).isEqualTo(expectedMoovOffset);
        }

        @Test
        @DisplayName("moov가 없으면 empty가 반환된다")
        void findMoovBox_noMoovBox_returnsEmpty() {
            byte[] data = buildFtyp();

            Optional<Mp4BoxHeader> result = Mp4Parser.findMoovBox(data);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("mdat가 없으면 Mp4ParseException이 발생한다")
        void inferMoovOffsetAfterMdat_noMdatBox_throwsMp4ParseException() {
            byte[] data = buildFtyp();

            assertThatThrownBy(() -> Mp4Parser.inferMoovOffsetAfterMdat(data, data.length))
                    .isInstanceOf(Mp4ParseException.class)
                    .hasMessageContaining("Cannot locate mdat box");
        }
    }

    // ── parseVideoTrack ──────────────────────────────────────────────

    @Nested
    @DisplayName("parseVideoTrack")
    class ParseVideoTrackTest {

        @Test
        @DisplayName("완전한 moov 구조에서 비디오 트랙 정보가 정확히 추출된다")
        void parseVideoTrack_completeMoov_extractsAllTrackInfo() {
            // given
            VideoMoovParams params = new VideoMoovParams();
            byte[] moov = buildVideoMoov(params);

            // when
            VideoTrackInfo info = Mp4Parser.parseVideoTrack(moov);

            assertThat(info.timescale()).isEqualTo(30000);
            assertThat(info.durationMs()).isEqualTo(10000); // 300000 * 1000 / 30000 = 10000ms
            assertThat(info.width()).isEqualTo(1920);
            assertThat(info.height()).isEqualTo(1080);
            assertThat(info.codecType()).isEqualTo("avc1");
            assertThat(info.nalLengthSize()).isEqualTo(4);

            assertThat(info.codecConfig()).isNotNull();
            assertThat(info.codecConfig().length).isGreaterThan(0);

            assertThat(info.sampleTable()).isNotNull();
            assertThat(info.sampleTable().getTotalSampleCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("파싱된 트랙에서 키프레임을 검색하면 올바른 오프셋과 크기가 반환된다")
        void parseVideoTrack_completeMoov_calculatesCorrectKeyframeOffsets() {
            // given
            VideoMoovParams params = new VideoMoovParams();
            // chunk offsets: [1000, 81000]
            // chunk 1: samples 0-4 (sizes: 50000, 10000, 10000, 10000, 40000)
            // chunk 2: samples 5-9 (sizes: 10000, 10000, 10000, 35000, 10000)
            // sync samples: 1, 5, 9 (1-based) → 0, 4, 8 (0-based)
            byte[] moov = buildVideoMoov(params);
            VideoTrackInfo info = Mp4Parser.parseVideoTrack(moov);

            // when: 전체 범위 검색 (0~10000ms)
            List<KeyframeInfo> keyframes = info.findKeyframesInRange(0, 10000);

            // then
            assertThat(keyframes).hasSize(3);

            // 첫 번째 키프레임: sample 0 → chunk 1 offset + 0 = 1000
            KeyframeInfo kf0 = keyframes.get(0);
            assertThat(kf0.sampleIndex()).isEqualTo(0);
            assertThat(kf0.fileOffset()).isEqualTo(1000);
            assertThat(kf0.size()).isEqualTo(50000);
            assertThat(kf0.timestampMs()).isEqualTo(0);

            // 두 번째 키프레임: sample 4 → chunk 1 offset + sum(50000+10000+10000+10000) = 1000 + 80000 = 81000
            KeyframeInfo kf4 = keyframes.get(1);
            assertThat(kf4.sampleIndex()).isEqualTo(4);
            assertThat(kf4.fileOffset()).isEqualTo(1000 + 50000 + 10000 + 10000 + 10000);
            assertThat(kf4.size()).isEqualTo(40000);

            // 세 번째 키프레임: sample 8 → chunk 2 offset + sum(10000+10000+10000) = 81000 + 30000 = 111000
            KeyframeInfo kf8 = keyframes.get(2);
            assertThat(kf8.sampleIndex()).isEqualTo(8);
            assertThat(kf8.fileOffset()).isEqualTo(81000 + 10000 + 10000 + 10000);
            assertThat(kf8.size()).isEqualTo(35000);
        }

        @Test
        @DisplayName("avcC에서 nalLengthSize와 codecConfig가 정확히 추출된다")
        void parseVideoTrack_avcCBox_extractsNalLengthSizeAndCodecConfig() {
            // given: nalLengthSizeMinusOne = 3 → nalLengthSize = 4
            VideoMoovParams params = new VideoMoovParams();
            params.nalLengthSizeMinusOne = 3;
            byte[] moov = buildVideoMoov(params);

            // when
            VideoTrackInfo info = Mp4Parser.parseVideoTrack(moov);

            // then
            assertThat(info.nalLengthSize()).isEqualTo(4);

            byte[] config = info.codecConfig();
            assertThat(config[0]).isEqualTo((byte) 1); // configurationVersion
            assertThat(config[4] & 0x03).isEqualTo(3); // nalLengthSizeMinusOne
        }

        @Test
        @DisplayName("mdhd version 1에서 timescale과 duration이 정확히 추출된다")
        void parseVideoTrack_mdhdVersion1_extractsTimescaleAndDuration() {
            // given: mdhd v1 (creation/modification 8바이트, duration 8바이트)
            VideoMoovParams params = new VideoMoovParams();
            params.mdhdVersion = 1;
            params.timescale = 90000;
            params.durationTicks = 900000; // 10초 at 90000
            byte[] moov = buildVideoMoov(params);

            // when
            VideoTrackInfo info = Mp4Parser.parseVideoTrack(moov);

            // then
            assertThat(info.timescale()).isEqualTo(90000);
            assertThat(info.durationMs()).isEqualTo(10000); // 900000 * 1000 / 90000
            assertThat(info.width()).isEqualTo(1920);
            assertThat(info.height()).isEqualTo(1080);
            assertThat(info.codecType()).isEqualTo("avc1");
        }

        @Test
        @DisplayName("비디오 트랙이 없고 오디오만 있으면 Mp4ParseException이 발생한다")
        void parseVideoTrack_audioOnlyMoov_throwsMp4ParseException() {
            byte[] moov = buildAudioOnlyMoov();

            assertThatThrownBy(() -> Mp4Parser.parseVideoTrack(moov))
                    .isInstanceOf(Mp4ParseException.class)
                    .hasMessageContaining("No video track found");
        }

        @Test
        @DisplayName("stsd가 누락되면 Mp4ParseException이 발생한다")
        void parseVideoTrack_missingStsd_throwsMp4ParseException() {
            byte[] moov = buildMoovMissingBox("stsd");

            assertThatThrownBy(() -> Mp4Parser.parseVideoTrack(moov))
                    .isInstanceOf(Mp4ParseException.class)
                    .hasMessageContaining("stsd box not found");
        }

        @Test
        @DisplayName("stts가 누락되면 Mp4ParseException이 발생한다")
        void parseVideoTrack_missingStts_throwsMp4ParseException() {
            byte[] moov = buildMoovMissingBox("stts");

            assertThatThrownBy(() -> Mp4Parser.parseVideoTrack(moov))
                    .isInstanceOf(Mp4ParseException.class)
                    .hasMessageContaining("stts not found");
        }

        @Test
        @DisplayName("stsz가 누락되면 Mp4ParseException이 발생한다")
        void parseVideoTrack_missingStsz_throwsMp4ParseException() {
            byte[] moov = buildMoovMissingBox("stsz");

            assertThatThrownBy(() -> Mp4Parser.parseVideoTrack(moov))
                    .isInstanceOf(Mp4ParseException.class)
                    .hasMessageContaining("stsz not found");
        }

        @Test
        @DisplayName("stco가 누락되면 Mp4ParseException이 발생한다")
        void parseVideoTrack_missingStco_throwsMp4ParseException() {
            byte[] moov = buildMoovMissingBox("stco");

            assertThatThrownBy(() -> Mp4Parser.parseVideoTrack(moov))
                    .isInstanceOf(Mp4ParseException.class)
                    .hasMessageContaining("stco/co64 not found");
        }

        @Test
        @DisplayName("co64를 사용해도 chunk offset이 정확히 파싱된다")
        void parseVideoTrack_co64ChunkOffsets_parsesCorrectly() {
            // given: 4GB 이상 오프셋을 시뮬레이션하기 위해 co64 사용
            VideoMoovParams params = new VideoMoovParams();
            params.useCo64 = true;
            byte[] moov = buildVideoMoov(params);

            // when
            VideoTrackInfo info = Mp4Parser.parseVideoTrack(moov);

            List<KeyframeInfo> keyframes = info.findKeyframesInRange(0, 10000);
            assertThat(keyframes).hasSize(3);

            // 첫 번째 키프레임: sample 0 → chunk offset 1000
            assertThat(keyframes.get(0).fileOffset()).isEqualTo(1000);
            // 세 번째 키프레임: sample 8 → chunk 2 offset(81000) + sum(sample 5~7)
            assertThat(keyframes.get(2).fileOffset()).isEqualTo(81000 + 10000 + 10000 + 10000);
        }
    }
}
