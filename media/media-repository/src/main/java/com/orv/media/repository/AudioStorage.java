package com.orv.media.repository;

import java.io.InputStream;

public interface AudioStorage {
    /**
     * S3에 오디오 파일을 업로드합니다.
     * @param audioStream 오디오 파일 스트림
     * @param contentType 콘텐츠 타입 (예: "audio/ogg; codecs=opus")
     * @param contentLength 파일 크기
     * @return 업로드된 파일의 file key (예: "archive/audios/{uuid}")
     */
    String save(InputStream audioStream, String contentType, long contentLength);

    /**
     * S3에서 오디오 파일을 삭제합니다.
     * @param fileKey 삭제할 파일의 file key
     */
    void delete(String fileKey);
}
