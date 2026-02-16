package com.orv.archive.service.infrastructure.mp4;

/**
 * 키프레임(sync sample)의 위치 정보.
 *
 * @param sampleIndex 0-based sample index
 * @param timestampMs 밀리초 단위 타임스탬프
 * @param fileOffset  파일 내 바이트 오프셋
 * @param size        sample 바이트 크기
 */
public record KeyframeInfo(
        int sampleIndex,
        long timestampMs,
        long fileOffset,
        int size
) {
}
