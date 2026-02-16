package com.orv.archive.repository;

/**
 * 비디오 파일의 바이트 레벨 읽기 인터페이스.
 * MP4 파싱 등 파일 내용에 직접 접근해야 하는 경우에 사용한다.
 */
public interface VideoFileReader {
    long getFileSize(String fileKey);

    byte[] getRange(String fileKey, long offset, long length);
}
