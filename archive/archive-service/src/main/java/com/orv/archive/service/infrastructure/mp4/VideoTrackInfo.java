package com.orv.archive.service.infrastructure.mp4;

import java.util.List;

/**
 * MP4 비디오 트랙의 파싱된 메타데이터.
 *
 * @param timescale   mdhd timescale
 * @param durationMs  영상 길이
 * @param width       비디오 너비
 * @param height      비디오 높이
 * @param codecType   코덱 타입 (예: "avc1", "hev1")
 * @param codecConfig 디코더 설정 바이트 (avcC/hvcC raw bytes)
 * @param nalLengthSize AVCC NAL length prefix 크기
 * @param sampleTable 파싱된 sample table
 */
public record VideoTrackInfo(
        long timescale,
        long durationMs,
        int width,
        int height,
        String codecType,
        byte[] codecConfig,
        int nalLengthSize,
        SampleTableInfo sampleTable
) {
    /**
     * 주어진 시간 범위 내의 키프레임들을 찾아 반환한다.
     */
    public List<KeyframeInfo> findKeyframesInRange(long startMs, long endMs) {
        return sampleTable.findKeyframesInRange(startMs, endMs, timescale);
    }

    /**
     * 특정 시간에 가장 가까운 키프레임을 찾는다.
     */
    public KeyframeInfo findNearestKeyframe(long targetMs) {
        return sampleTable.findNearestKeyframe(targetMs, timescale);
    }
}
