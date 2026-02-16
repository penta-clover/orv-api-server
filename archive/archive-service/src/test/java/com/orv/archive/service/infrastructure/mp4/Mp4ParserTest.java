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
        @DisplayName("일반 8바이트 헤더에서 type, offset, totalSize, headerSize 정확 추출")
        void normalHeader() {
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
        @DisplayName("extended size (size==1) 16바이트 헤더 정확 파싱")
        void extendedSizeHeader() {
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
        @DisplayName("지정된 offset에서 box 헤더 읽기")
        void readAtOffset() {
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
        @DisplayName("데이터가 8바이트 미만이면 Mp4ParseException 발생")
        void insufficientData() {
            byte[] data = new byte[]{0x00, 0x00, 0x00};

            assertThatThrownBy(() -> Mp4Parser.readBoxHeader(data, 0))
                    .isInstanceOf(Mp4ParseException.class)
                    .hasMessageContaining("Not enough data");
        }

        @Test
        @DisplayName("offset이 데이터 끝을 넘어서면 Mp4ParseException 발생")
        void offsetBeyondData() {
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
        @DisplayName("ftyp → moov → mdat 순서의 top-level box들을 정확히 스캔")
        void scanFtypMoovMdat() {
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

            // offset 연속 확인
            assertThat(boxes.get(0).offset()).isEqualTo(0);
            assertThat(boxes.get(1).offset()).isEqualTo(ftyp.length);
            assertThat(boxes.get(2).offset()).isEqualTo(ftyp.length + moov.length);
        }

        @Test
        @DisplayName("빈 데이터에서 빈 리스트 반환")
        void emptyData() {
            List<Mp4BoxHeader> boxes = Mp4Parser.scanTopLevelBoxes(new byte[0]);
            assertThat(boxes).isEmpty();
        }
    }

    // ── findMoovBox ──────────────────────────────────────────────────

    @Nested
    @DisplayName("findMoovBox")
    class FindMoovBoxTest {

        @Test
        @DisplayName("moov가 앞에 있을 때 정확히 찾기")
        void moovAtFront() {
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
        @DisplayName("moov가 mdat 뒤에 있을 때 mdat 크기로부터 위치 추정")
        void moovAfterMdat() {
            // given: probe에 ftyp + mdat 헤더만 있고, moov는 mdat 뒤에 위치
            byte[] ftyp = buildFtyp();
            int mdatContentSize = 100000;
            long mdatTotalSize = 8 + mdatContentSize;
            byte[] mdatHeader = concat(uint32(mdatTotalSize), ascii("mdat"));
            byte[] probeData = concat(ftyp, mdatHeader);

            long expectedMoovOffset = ftyp.length + mdatTotalSize;
            long fileSize = expectedMoovOffset + 500;

            // when
            Optional<Mp4BoxHeader> moovResult = Mp4Parser.findMoovBox(probeData);
            long inferredOffset = Mp4Parser.inferMoovOffsetAfterMdat(probeData, fileSize);

            // then
            assertThat(moovResult).isEmpty();  // 프로브 데이터에는 moov 없음
            assertThat(inferredOffset).isEqualTo(expectedMoovOffset);
        }

        @Test
        @DisplayName("moov가 없으면 empty 반환")
        void noMoov() {
            byte[] data = buildFtyp();

            Optional<Mp4BoxHeader> result = Mp4Parser.findMoovBox(data);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("mdat도 없으면 inferMoovOffsetAfterMdat이 예외 발생")
        void noMdatForInference() {
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
        @DisplayName("완전한 moov 구조에서 비디오 트랙 정보 정확 추출")
        void fullMoovStructure() {
            // given
            VideoMoovParams params = new VideoMoovParams();
            byte[] moov = buildVideoMoov(params);

            // when
            VideoTrackInfo info = Mp4Parser.parseVideoTrack(moov);

            // then: 기본 메타데이터
            assertThat(info.timescale()).isEqualTo(30000);
            assertThat(info.durationMs()).isEqualTo(10000); // 300000 * 1000 / 30000 = 10000ms
            assertThat(info.width()).isEqualTo(1920);
            assertThat(info.height()).isEqualTo(1080);
            assertThat(info.codecType()).isEqualTo("avc1");
            assertThat(info.nalLengthSize()).isEqualTo(4);

            // then: codecConfig (avcC raw bytes)
            assertThat(info.codecConfig()).isNotNull();
            assertThat(info.codecConfig().length).isGreaterThan(0);

            // then: sample table
            assertThat(info.sampleTable()).isNotNull();
            assertThat(info.sampleTable().getTotalSampleCount()).isEqualTo(10);
            assertThat(info.sampleTable().getSyncSamples()).containsExactly(1, 5, 9);
        }

        @Test
        @DisplayName("파싱된 트랙에서 키프레임 검색이 올바른 오프셋과 크기를 반환")
        void keyframeOffsetCalculation() {
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
        @DisplayName("avcC에서 nalLengthSize와 codecConfig 정확 추출")
        void avcCExtraction() {
            // given: nalLengthSizeMinusOne = 3 → nalLengthSize = 4
            VideoMoovParams params = new VideoMoovParams();
            params.nalLengthSizeMinusOne = 3;
            byte[] moov = buildVideoMoov(params);

            // when
            VideoTrackInfo info = Mp4Parser.parseVideoTrack(moov);

            // then
            assertThat(info.nalLengthSize()).isEqualTo(4);

            // codecConfig에서 SPS 시작 바이트 확인
            // avcC 구조: configVersion(1) + profile(1) + compat(1) + level(1) + nalLengthSizeMinusOne(1) + ...
            byte[] config = info.codecConfig();
            assertThat(config[0]).isEqualTo((byte) 1); // configurationVersion
            assertThat(config[4] & 0x03).isEqualTo(3); // nalLengthSizeMinusOne
        }

        @Test
        @DisplayName("mdhd version 1에서 timescale과 duration 정확 추출")
        void mdhdVersion1() {
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
        @DisplayName("비디오 트랙이 없으면 (오디오만) Mp4ParseException 발생")
        void noVideoTrack() {
            byte[] moov = buildAudioOnlyMoov();

            assertThatThrownBy(() -> Mp4Parser.parseVideoTrack(moov))
                    .isInstanceOf(Mp4ParseException.class)
                    .hasMessageContaining("No video track found");
        }

        @Test
        @DisplayName("stsd 누락 시 Mp4ParseException 발생")
        void missingStsd() {
            byte[] moov = buildMoovMissingBox("stsd");

            assertThatThrownBy(() -> Mp4Parser.parseVideoTrack(moov))
                    .isInstanceOf(Mp4ParseException.class)
                    .hasMessageContaining("stsd box not found");
        }

        @Test
        @DisplayName("stts 누락 시 Mp4ParseException 발생")
        void missingStts() {
            byte[] moov = buildMoovMissingBox("stts");

            assertThatThrownBy(() -> Mp4Parser.parseVideoTrack(moov))
                    .isInstanceOf(Mp4ParseException.class)
                    .hasMessageContaining("stts not found");
        }

        @Test
        @DisplayName("stsz 누락 시 Mp4ParseException 발생")
        void missingStsz() {
            byte[] moov = buildMoovMissingBox("stsz");

            assertThatThrownBy(() -> Mp4Parser.parseVideoTrack(moov))
                    .isInstanceOf(Mp4ParseException.class)
                    .hasMessageContaining("stsz not found");
        }

        @Test
        @DisplayName("stco 누락 시 Mp4ParseException 발생")
        void missingStco() {
            byte[] moov = buildMoovMissingBox("stco");

            assertThatThrownBy(() -> Mp4Parser.parseVideoTrack(moov))
                    .isInstanceOf(Mp4ParseException.class)
                    .hasMessageContaining("stco/co64 not found");
        }
    }
}
