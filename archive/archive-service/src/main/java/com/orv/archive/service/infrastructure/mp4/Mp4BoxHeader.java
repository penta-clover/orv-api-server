package com.orv.archive.service.infrastructure.mp4;

/**
 * MP4 box(atom) 헤더 정보.
 *
 * @param type       4글자 box 타입 (예: "ftyp", "moov", "mdat")
 * @param offset     파일(또는 부모 box) 내 이 box의 시작 위치
 * @param totalSize  box 전체 크기 (헤더 포함)
 * @param headerSize 헤더 크기 (보통 8, extended size이면 16)
 */
public record Mp4BoxHeader(
        String type,
        long offset,
        long totalSize,
        int headerSize
) {
    public long dataOffset() {
        return offset + headerSize;
    }

    public long dataSize() {
        return totalSize - headerSize;
    }
}
