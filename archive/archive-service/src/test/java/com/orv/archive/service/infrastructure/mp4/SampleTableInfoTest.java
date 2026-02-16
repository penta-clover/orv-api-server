package com.orv.archive.service.infrastructure.mp4;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SampleTableInfoTest {

    /**
     * 테스트 데이터 시나리오:
     * - timescale = 30000, sampleDelta = 1000 → 각 sample = 33.33ms
     * - 10 samples (0-based: 0~9)
     * - 2 chunks: chunk 1 (samples 0-4), chunk 2 (samples 5-9)
     * - chunk offsets: [1000, 81000]
     * - sample sizes: [50000, 10000, 10000, 10000, 40000, 10000, 10000, 10000, 35000, 10000]
     * - sync samples (1-based): {1, 5, 9} → (0-based: 0, 4, 8)
     *
     * 타임스탬프 계산:
     * sample 0: 0ms
     * sample 1: 33.33ms
     * sample 4: 133.33ms
     * sample 8: 266.66ms
     * sample 9: 300ms
     * 총 duration: 10 * 1000 / 30000 * 1000 = 333.33ms
     */
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
        @DisplayName("전체 범위에서 모든 키프레임 반환")
        void allKeyframes() {
            List<KeyframeInfo> keyframes = sampleTable.findKeyframesInRange(0, 400, TIMESCALE);

            assertThat(keyframes).hasSize(3);
            assertThat(keyframes.get(0).sampleIndex()).isEqualTo(0);
            assertThat(keyframes.get(1).sampleIndex()).isEqualTo(4);
            assertThat(keyframes.get(2).sampleIndex()).isEqualTo(8);
        }

        @Test
        @DisplayName("0~150ms 범위에서 sample 0, 4 반환 (sample 4 = 133ms)")
        void partialRange() {
            // sample 4 timestamp = 4 * 1000 / 30000 * 1000 = 133.33ms
            List<KeyframeInfo> keyframes = sampleTable.findKeyframesInRange(0, 150, TIMESCALE);

            assertThat(keyframes).hasSize(2);
            assertThat(keyframes.get(0).sampleIndex()).isEqualTo(0);
            assertThat(keyframes.get(1).sampleIndex()).isEqualTo(4);
        }

        @Test
        @DisplayName("키프레임이 없는 범위에서 빈 리스트 반환")
        void noKeyframesInRange() {
            // sample 1 = 33ms, sample 3 = 100ms → 이 범위에 sync sample 없음 (sample 4 = 133ms는 범위 밖)
            List<KeyframeInfo> keyframes = sampleTable.findKeyframesInRange(34, 100, TIMESCALE);

            assertThat(keyframes).isEmpty();
        }

        @Test
        @DisplayName("syncSamples가 null이면 빈 리스트 반환")
        void nullSyncSamples() {
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
        @DisplayName("반환된 키프레임의 timestampMs가 정확")
        void timestampAccuracy() {
            List<KeyframeInfo> keyframes = sampleTable.findKeyframesInRange(0, 400, TIMESCALE);

            // sample 0: 0 * 1000 / 30000 * 1000 = 0ms
            assertThat(keyframes.get(0).timestampMs()).isEqualTo(0);
            // sample 4: 4000 * 1000 / 30000 = 133ms
            assertThat(keyframes.get(1).timestampMs()).isEqualTo(133);
            // sample 8: 8000 * 1000 / 30000 = 266ms
            assertThat(keyframes.get(2).timestampMs()).isEqualTo(266);
        }
    }

    // ── findNearestKeyframe ──────────────────────────────────────────

    @Nested
    @DisplayName("findNearestKeyframe")
    class FindNearestKeyframeTest {

        @Test
        @DisplayName("100ms에 가장 가까운 키프레임 → sample 4 (133ms)")
        void nearestToMiddle() {
            // targetMs=100 → targetSample=3 (100*30000/1000=3000 ticks, 3000/1000=3)
            // sync samples: 0(dist=3), 4(dist=1), 8(dist=5) → nearest is 4
            KeyframeInfo nearest = sampleTable.findNearestKeyframe(100, TIMESCALE);

            assertThat(nearest).isNotNull();
            assertThat(nearest.sampleIndex()).isEqualTo(4);
        }

        @Test
        @DisplayName("0ms에 가장 가까운 키프레임 → sample 0")
        void nearestToStart() {
            KeyframeInfo nearest = sampleTable.findNearestKeyframe(0, TIMESCALE);

            assertThat(nearest).isNotNull();
            assertThat(nearest.sampleIndex()).isEqualTo(0);
        }

        @Test
        @DisplayName("300ms에 가장 가까운 키프레임 → sample 8 (266ms)")
        void nearestToEnd() {
            KeyframeInfo nearest = sampleTable.findNearestKeyframe(300, TIMESCALE);

            assertThat(nearest).isNotNull();
            assertThat(nearest.sampleIndex()).isEqualTo(8);
        }

        @Test
        @DisplayName("syncSamples가 null이면 null 반환")
        void nullSyncSamples() {
            SampleTableInfo noSync = new SampleTableInfo(
                    List.of(new SampleTableInfo.SttsEntry(10, 1000)),
                    null,
                    List.of(new SampleTableInfo.StscEntry(1, 5, 1)),
                    new int[]{50000, 10000, 10000, 10000, 40000, 10000, 10000, 10000, 35000, 10000},
                    new long[]{1000, 81000}
            );

            assertThat(noSync.findNearestKeyframe(100, TIMESCALE)).isNull();
        }
    }

    // ── sampleFileOffset ─────────────────────────────────────────────

    @Nested
    @DisplayName("sampleFileOffset")
    class SampleFileOffsetTest {

        @Test
        @DisplayName("chunk 1의 첫 sample (index 0) → chunk offset 그대로")
        void firstSampleFirstChunk() {
            long offset = sampleTable.sampleFileOffset(0);
            assertThat(offset).isEqualTo(1000);
        }

        @Test
        @DisplayName("chunk 1의 두 번째 sample (index 1) → chunk offset + sample 0 크기")
        void secondSampleFirstChunk() {
            long offset = sampleTable.sampleFileOffset(1);
            assertThat(offset).isEqualTo(1000 + 50000);
        }

        @Test
        @DisplayName("chunk 1의 마지막 sample (index 4) → chunk offset + sum(sample 0~3)")
        void lastSampleFirstChunk() {
            long offset = sampleTable.sampleFileOffset(4);
            assertThat(offset).isEqualTo(1000 + 50000 + 10000 + 10000 + 10000);
        }

        @Test
        @DisplayName("chunk 2의 첫 sample (index 5) → chunk 2 offset 그대로")
        void firstSampleSecondChunk() {
            long offset = sampleTable.sampleFileOffset(5);
            assertThat(offset).isEqualTo(81000);
        }

        @Test
        @DisplayName("chunk 2의 네 번째 sample (index 8) → chunk 2 offset + sum(sample 5~7)")
        void fourthSampleSecondChunk() {
            long offset = sampleTable.sampleFileOffset(8);
            assertThat(offset).isEqualTo(81000 + 10000 + 10000 + 10000);
        }

        @Test
        @DisplayName("복수 stsc 엔트리에서도 올바른 오프셋 계산")
        void multipleStscEntries() {
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
