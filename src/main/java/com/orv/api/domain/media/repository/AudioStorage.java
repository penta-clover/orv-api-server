package com.orv.api.domain.media.repository;

import java.io.InputStream;
import java.util.UUID;

public interface AudioStorage {
    /**
     * S3에 오디오 파일을 업로드합니다.
     * @param audioStream 오디오 파일 스트림
     * @param contentType 콘텐츠 타입 (예: "audio/ogg; codecs=opus")
     * @param contentLength 파일 크기
     * @return 업로드된 파일의 ID
     */
    UUID save(InputStream audioStream, String contentType, long contentLength);

    /**
     * S3에서 오디오 파일을 삭제합니다.
     * @param fileId 삭제할 파일 ID
     */
    void delete(UUID fileId);

    /**
     * 파일 ID를 CloudFront URL로 변환합니다.
     * @param fileId 파일 ID
     * @return CloudFront URL
     */
    String getUrl(UUID fileId);
}
