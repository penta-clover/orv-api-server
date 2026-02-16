package com.orv.worker.thumbnailextraction.service;

import java.awt.image.BufferedImage;

/**
 * 비디오 프레임 디코더 인터페이스.
 * 코덱별 구현체를 통해 확장 가능하다.
 */
public interface FrameDecoder {

    /**
     * 인코딩된 sample 데이터를 디코딩하여 이미지를 반환한다.
     *
     * @param codecConfig   디코더 설정 바이트 (예: avcC, hvcC)
     * @param sampleData    인코딩된 sample 바이트 (AVCC 포맷)
     * @param nalLengthSize NAL unit length prefix 크기 (보통 4)
     * @param width         비디오 너비 (디코딩 버퍼 할당용)
     * @param height        비디오 높이 (디코딩 버퍼 할당용)
     * @return 디코딩된 이미지
     */
    BufferedImage decode(byte[] codecConfig, byte[] sampleData, int nalLengthSize, int width, int height);

    /**
     * 주어진 코덱 타입을 지원하는지 확인한다.
     *
     * @param codecType stsd의 코덱 타입 (예: "avc1", "hev1")
     */
    boolean supports(String codecType);
}
