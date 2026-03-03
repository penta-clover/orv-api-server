package com.orv.archive.service.infrastructure.mp4;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SampleTableInfoTest {

    private static final long TIMESCALE = 30000;

    private SampleTableInfo sampleTable;

    @BeforeEach
    void setUp() {
        sampleTable = new SampleTableInfo(
                List.of(new SampleTableInfo.SttsEntry(10, 1000)),
                new int[]{1, 5, 9},  // 1-based sync samples
                List.of(new SampleTableInfo.StscEntry(1, 5, 1)), // all chunks: 5 samples per chunk
                new int[]{50000, 10000, 10000, 10000, 40000, 10000, 10000, 10000, 35000, 10000},
                new long[]{1000, 81000}  // 2 chunk offsets
        );
    }

    // ── findKeyframesInRange ─────────────────────────────────────────

    @Nested
    @DisplayName("findKeyframesInRange")
    class FindKeyframesInRangeTest {

        @Test
        @DisplayName("전체 범위를 조회하면 모든 키프레임이 반환된다")
        void findKeyframesInRange_fullRange_returnsAllKeyframes() {
            List<KeyframeInfo> keyframes = sampleTable.findKeyframesInRange(0, 400, TIMESCALE);

            assertThat(keyframes).hasSize(3);
            assertThat(keyframes.get(0).sampleIndex()).isEqualTo(0);
            assertThat(keyframes.get(1).sampleIndex()).isEqualTo(4);
            assertThat(keyframes.get(2).sampleIndex()).isEqualTo(8);
        }

        @Test
        @DisplayName("0~150ms 범위를 조회하면 해당 범위의 키프레임만 반환된다")
        void findKeyframesInRange_partialRange_returnsKeyframesInRange() {
            // sample 4 timestamp = 4 * 1000 / 30000 * 1000 = 133.33ms
            List<KeyframeInfo> keyframes = sampleTable.findKeyframesInRange(0, 150, TIMESCALE);

            assertThat(keyframes).hasSize(2);
            assertThat(keyframes.get(0).sampleIndex()).isEqualTo(0);
            assertThat(keyframes.get(1).sampleIndex()).isEqualTo(4);
        }

        @Test
        @DisplayName("키프레임이 없는 범위를 조회하면 빈 리스트가 반환된다")
        void findKeyframesInRange_rangeWithoutKeyframes_returnsEmptyList() {
            // sample 1 = 33ms, sample 3 = 100ms → 이 범위에 sync sample 없음 (sample 4 = 133ms는 범위 밖)
            List<KeyframeInfo> keyframes = sampleTable.findKeyframesInRange(34, 100, TIMESCALE);

            assertThat(keyframes).isEmpty();
        }

        @Test
        @DisplayName("syncSamples가 null이면 빈 리스트가 반환된다")
        void findKeyframesInRange_nullSyncSamples_returnsEmptyList() {
            SampleTableInfo noSync = new SampleTableInfo(
                    List.of(new SampleTableInfo.SttsEntry(10, 1000)),
                    null,
                    List.of(new SampleTableInfo.StscEntry(1, 5, 1)),
                    new int[]{50000, 10000, 10000, 10000, 40000, 10000, 10000, 10000, 35000, 10000},
                    new long[]{1000, 81000}
            );

            List<KeyframeInfo> keyframes = noSync.findKeyframesInRange(0, 400, TIMESCALE);
            assertThat(keyframes).isEmpty();
        }

        @Test
        @DisplayName("반환된 키프레임의 timestampMs가 정확하다")
        void findKeyframesInRange_allKeyframes_returnsAccurateTimestamps() {
            List<KeyframeInfo> keyframes = sampleTable.findKeyframesInRange(0, 400, TIMESCALE);

            // sample 0: 0 * 1000 / 30000 * 1000 = 0ms
            assertThat(keyframes.get(0).timestampMs()).isEqualTo(0);
            // sample 4: 4000 * 1000 / 30000 = 133ms
            assertThat(keyframes.get(1).timestampMs()).isEqualTo(133);
            // sample 8: 8000 * 1000 / 30000 = 266ms
            assertThat(keyframes.get(2).timestampMs()).isEqualTo(266);
        }
    }

    // ── sampleFileOffset ─────────────────────────────────────────────

    @Nested
    @DisplayName("sampleFileOffset")
    class SampleFileOffsetTest {

        @Test
        @DisplayName("chunk 1의 첫 sample이면 chunk offset이 그대로 반환된다")
        void sampleFileOffset_firstSampleInChunk1_returnsChunkOffset() {
            long offset = sampleTable.sampleFileOffset(0);
            assertThat(offset).isEqualTo(1000);
        }

        @Test
        @DisplayName("chunk 1의 두 번째 sample이면 앞선 sample 크기가 더해진다")
        void sampleFileOffset_secondSampleInChunk1_addsPrecedingSampleSize() {
            long offset = sampleTable.sampleFileOffset(1);
            assertThat(offset).isEqualTo(1000 + 50000);
        }

        @Test
        @DisplayName("chunk 1의 마지막 sample이면 앞선 모든 sample 크기가 더해진다")
        void sampleFileOffset_lastSampleInChunk1_addsAllPrecedingSizes() {
            long offset = sampleTable.sampleFileOffset(4);
            assertThat(offset).isEqualTo(1000 + 50000 + 10000 + 10000 + 10000);
        }

        @Test
        @DisplayName("chunk 2의 첫 sample이면 chunk 2 offset이 그대로 반환된다")
        void sampleFileOffset_firstSampleInChunk2_returnsChunk2Offset() {
            long offset = sampleTable.sampleFileOffset(5);
            assertThat(offset).isEqualTo(81000);
        }

        @Test
        @DisplayName("chunk 2의 네 번째 sample이면 앞선 sample 크기가 더해진다")
        void sampleFileOffset_fourthSampleInChunk2_addsPrecedingSizes() {
            long offset = sampleTable.sampleFileOffset(8);
            assertThat(offset).isEqualTo(81000 + 10000 + 10000 + 10000);
        }

        @Test
        @DisplayName("범위를 벗어난 sampleIndex를 조회하면 IllegalArgumentException이 발생한다")
        void sampleFileOffset_indexOutOfRange_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> sampleTable.sampleFileOffset(10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sample index out of range");
        }

        @Test
        @DisplayName("복수 stsc 엔트리가 있어도 올바른 오프셋이 계산된다")
        void sampleFileOffset_multipleStscEntries_calculatesCorrectOffsets() {
            // stsc: chunk 1-1 has 3 samples, chunk 2+ has 2 samples
            // 총 7 samples, 3 chunks
            SampleTableInfo multi = new SampleTableInfo(
                    List.of(new SampleTableInfo.SttsEntry(7, 1000)),
                    new int[]{1, 4, 6},
                    List.of(
                            new int[]{1, 3, 1},  // chunk 1: 3 samples (0,1,2)
                            new int[]{2, 2, 1}   // chunk 2+: 2 samples each (3,4 | 5,6)
                    ).stream().map(e -> new SampleTableInfo.StscEntry(e[0], e[1], e[2])).toList(),
                    new int[]{100, 200, 300, 400, 500, 600, 700},
                    new long[]{5000, 6000, 7000}  // 3 chunks
            );

            // sample 0: chunk 0 offset = 5000
            assertThat(multi.sampleFileOffset(0)).isEqualTo(5000);
            // sample 2: chunk 0 offset + 100 + 200 = 5300
            assertThat(multi.sampleFileOffset(2)).isEqualTo(5000 + 100 + 200);
            // sample 3: chunk 1 offset = 6000
            assertThat(multi.sampleFileOffset(3)).isEqualTo(6000);
            // sample 4: chunk 1 offset + 400 = 6400
            assertThat(multi.sampleFileOffset(4)).isEqualTo(6000 + 400);
            // sample 5: chunk 2 offset = 7000
            assertThat(multi.sampleFileOffset(5)).isEqualTo(7000);
            // sample 6: chunk 2 offset + 600 = 7600
            assertThat(multi.sampleFileOffset(6)).isEqualTo(7000 + 600);
        }
    }
}
