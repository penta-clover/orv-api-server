package com.orv.archive.service.infrastructure.mp4;

import java.util.ArrayList;
import java.util.List;

/**
 * MP4 sample table (stbl) 파싱 결과.
 * stts, stss, stsc, stsz, stco/co64 데이터를 보유한다.
 */
public class SampleTableInfo {

    /** stts 엔트리: (sampleCount, sampleDelta) */
    public record SttsEntry(int sampleCount, int sampleDelta) {}

    /** stsc 엔트리: (firstChunk, samplesPerChunk, sampleDescriptionIndex) — 모두 1-based */
    public record StscEntry(int firstChunk, int samplesPerChunk, int sampleDescriptionIndex) {}

    private final List<SttsEntry> sttsEntries;
    private final int[] syncSamples;        // stss: 1-based sync sample indices
    private final List<StscEntry> stscEntries;
    private final int[] sampleSizes;        // stsz: 각 sample 크기 (0-based index)
    private final long[] chunkOffsets;      // stco/co64: 각 chunk의 파일 내 오프셋 (0-based index)
    private final int totalSampleCount;

    public SampleTableInfo(
            List<SttsEntry> sttsEntries,
            int[] syncSamples,
            List<StscEntry> stscEntries,
            int[] sampleSizes,
            long[] chunkOffsets
    ) {
        this.sttsEntries = sttsEntries;
        this.syncSamples = syncSamples;
        this.stscEntries = stscEntries;
        this.sampleSizes = sampleSizes;
        this.chunkOffsets = chunkOffsets;
        this.totalSampleCount = sampleSizes.length;
    }

    /**
     * 주어진 시간 범위 내의 키프레임들을 찾아 반환한다.
     *
     * @param startMs 시작 시간 (ms, inclusive)
     * @param endMs   종료 시간 (ms, inclusive)
     * @param timescale 비디오 트랙 timescale
     * @return 키프레임 정보 목록
     */
    public List<KeyframeInfo> findKeyframesInRange(long startMs, long endMs, long timescale) {
        if (syncSamples == null || syncSamples.length == 0) {
            return List.of();
        }

        int startSample = sampleIndexAtTime(startMs, timescale);
        int endSample = sampleIndexAtTime(endMs, timescale);

        List<KeyframeInfo> result = new ArrayList<>();
        for (int syncSample1Based : syncSamples) {
            int idx = syncSample1Based - 1; // 0-based
            if (idx < startSample) continue;
            if (idx > endSample) break;

            long timestampMs = sampleTimestampMs(idx, timescale);
            long fileOffset = sampleFileOffset(idx);
            int size = sampleSizes[idx];

            result.add(new KeyframeInfo(idx, timestampMs, fileOffset, size));
        }
        return result;
    }

    /**
     * 가장 가까운 키프레임을 반환한다 (범위 내에 키프레임이 없을 때 사용).
     */
    public KeyframeInfo findNearestKeyframe(long targetMs, long timescale) {
        if (syncSamples == null || syncSamples.length == 0) {
            return null;
        }

        int targetSample = sampleIndexAtTime(targetMs, timescale);
        int bestSync = syncSamples[0];
        int bestDist = Math.abs((bestSync - 1) - targetSample);

        for (int sync1Based : syncSamples) {
            int dist = Math.abs((sync1Based - 1) - targetSample);
            if (dist < bestDist) {
                bestDist = dist;
                bestSync = sync1Based;
            } else if (dist > bestDist) {
                break; // stss는 정렬되어 있으므로 거리가 커지면 중단
            }
        }

        int idx = bestSync - 1;
        return new KeyframeInfo(
                idx,
                sampleTimestampMs(idx, timescale),
                sampleFileOffset(idx),
                sampleSizes[idx]
        );
    }

    /**
     * stts를 이용하여 특정 시간(ms)에 해당하는 sample index를 구한다.
     */
    private int sampleIndexAtTime(long timeMs, long timescale) {
        long targetTicks = timeMs * timescale / 1000;
        long accTicks = 0;
        int accSamples = 0;

        for (SttsEntry entry : sttsEntries) {
            long entryTotalTicks = (long) entry.sampleCount() * entry.sampleDelta();
            if (accTicks + entryTotalTicks > targetTicks) {
                int samplesInto = (int) ((targetTicks - accTicks) / entry.sampleDelta());
                return Math.min(accSamples + samplesInto, totalSampleCount - 1);
            }
            accTicks += entryTotalTicks;
            accSamples += entry.sampleCount();
        }
        return totalSampleCount - 1;
    }

    /**
     * 특정 sample의 타임스탬프(ms)를 계산한다.
     */
    private long sampleTimestampMs(int sampleIndex, long timescale) {
        long ticks = 0;
        int remaining = sampleIndex;

        for (SttsEntry entry : sttsEntries) {
            if (remaining <= 0) break;
            int count = Math.min(remaining, entry.sampleCount());
            ticks += (long) count * entry.sampleDelta();
            remaining -= count;
        }
        return ticks * 1000 / timescale;
    }

    /**
     * stsc + stco + stsz를 이용하여 특정 sample의 파일 내 바이트 오프셋을 계산한다.
     *
     * 알고리즘:
     * 1. stsc로 sample이 속한 chunk와 chunk 내 위치를 찾음
     * 2. stco로 chunk의 파일 오프셋을 구함
     * 3. stsz로 chunk 내 선행 sample들의 크기를 합산
     */
    public long sampleFileOffset(int sampleIndex) {
        // sample이 속한 chunk 번호(1-based)와 chunk 내 offset 계산
        int accumulatedSamples = 0;

        for (int i = 0; i < stscEntries.size(); i++) {
            StscEntry entry = stscEntries.get(i);
            int firstChunk0 = entry.firstChunk() - 1; // 0-based

            // 이 stsc 엔트리가 커버하는 chunk 범위
            int nextFirstChunk0;
            if (i + 1 < stscEntries.size()) {
                nextFirstChunk0 = stscEntries.get(i + 1).firstChunk() - 1;
            } else {
                nextFirstChunk0 = chunkOffsets.length;
            }

            int chunksInRange = nextFirstChunk0 - firstChunk0;
            int samplesInRange = chunksInRange * entry.samplesPerChunk();

            if (accumulatedSamples + samplesInRange > sampleIndex) {
                // 이 범위 내에 target sample이 있음
                int sampleInRange = sampleIndex - accumulatedSamples;
                int chunkWithinRange = sampleInRange / entry.samplesPerChunk();
                int sampleWithinChunk = sampleInRange % entry.samplesPerChunk();

                int chunkIndex0 = firstChunk0 + chunkWithinRange;
                long chunkOffset = chunkOffsets[chunkIndex0];

                // chunk 내 선행 sample 크기 합산
                long offsetInChunk = 0;
                int firstSampleInChunk = accumulatedSamples + chunkWithinRange * entry.samplesPerChunk();
                for (int s = firstSampleInChunk; s < firstSampleInChunk + sampleWithinChunk; s++) {
                    offsetInChunk += sampleSizes[s];
                }

                return chunkOffset + offsetInChunk;
            }

            accumulatedSamples += samplesInRange;
        }

        throw new IllegalArgumentException("Sample index out of range: " + sampleIndex);
    }

    public int getTotalSampleCount() {
        return totalSampleCount;
    }

    public int[] getSyncSamples() {
        return syncSamples;
    }
}
