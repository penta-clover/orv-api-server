package com.orv.worker.thumbnailextraction.service;

import lombok.extern.slf4j.Slf4j;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * JCodec 기반 H.264 I-frame 디코더.
 * avcC 설정 바이트와 AVCC 포맷 sample 데이터를 받아 BufferedImage를 반환한다.
 */
@Component
@Slf4j
public class H264FrameDecoder implements FrameDecoder {

    @Override
    public boolean supports(String codecType) {
        return "avc1".equals(codecType) || "avc3".equals(codecType);
    }

    @Override
    public BufferedImage decode(byte[] codecConfig, byte[] sampleData, int nalLengthSize, int width, int height) {
        // 1. avcC에서 SPS/PPS 추출
        AvcCData avcC = parseAvcC(codecConfig);

        // 2. 디코더 생성 및 SPS/PPS 등록
        H264Decoder decoder = new H264Decoder();
        decoder.addSps(avcC.spsList);
        decoder.addPps(avcC.ppsList);

        // 3. sample 데이터를 NAL unit 리스트로 분리 (AVCC → 개별 NAL)
        List<ByteBuffer> nalUnits = splitAvccNalUnits(sampleData, nalLengthSize);

        // 4. 실제 비디오 해상도로 디코딩 버퍼 할당
        // macroblock 정렬을 위해 16의 배수로 올림
        int alignedWidth = alignTo16(width);
        int alignedHeight = alignTo16(height);
        byte[][] buffer = Picture.create(alignedWidth, alignedHeight, ColorSpace.YUV420).getData();

        // 5. 디코딩
        Frame frame = decoder.decodeFrameFromNals(nalUnits, buffer);
        if (frame == null) {
            throw new RuntimeException("H264 decoding returned null frame");
        }

        // 6. Picture → BufferedImage 변환
        return AWTUtil.toBufferedImage(frame);
    }

    private static int alignTo16(int value) {
        return (value + 15) & ~15;
    }

    /**
     * avcC (AVC Decoder Configuration Record) 바이트를 파싱하여 SPS/PPS를 추출한다.
     *
     * avcC 구조:
     * - configurationVersion (1)
     * - AVCProfileIndication (1)
     * - profile_compatibility (1)
     * - AVCLevelIndication (1)
     * - lengthSizeMinusOne (1) [하위 2비트]
     * - numOfSequenceParameterSets (1) [하위 5비트]
     * - for each SPS: sequenceParameterSetLength (2) + sequenceParameterSetNALUnit (N)
     * - numOfPictureParameterSets (1)
     * - for each PPS: pictureParameterSetLength (2) + pictureParameterSetNALUnit (N)
     */
    private AvcCData parseAvcC(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);

        buf.get(); // configurationVersion
        buf.get(); // AVCProfileIndication
        buf.get(); // profile_compatibility
        buf.get(); // AVCLevelIndication
        buf.get(); // lengthSizeMinusOne (이미 Mp4Parser에서 추출함)

        // SPS
        int numSps = buf.get() & 0x1F;
        List<ByteBuffer> spsList = new ArrayList<>(numSps);
        for (int i = 0; i < numSps; i++) {
            int spsLength = (buf.get() & 0xFF) << 8 | (buf.get() & 0xFF);
            byte[] sps = new byte[spsLength];
            buf.get(sps);
            spsList.add(ByteBuffer.wrap(sps));
        }

        // PPS
        int numPps = buf.get() & 0xFF;
        List<ByteBuffer> ppsList = new ArrayList<>(numPps);
        for (int i = 0; i < numPps; i++) {
            int ppsLength = (buf.get() & 0xFF) << 8 | (buf.get() & 0xFF);
            byte[] pps = new byte[ppsLength];
            buf.get(pps);
            ppsList.add(ByteBuffer.wrap(pps));
        }

        return new AvcCData(spsList, ppsList);
    }

    /**
     * AVCC 포맷의 sample 데이터를 개별 NAL unit ByteBuffer 리스트로 분리한다.
     * AVCC 포맷: [nalLengthSize 바이트 길이][NAL unit 데이터] 반복
     */
    private List<ByteBuffer> splitAvccNalUnits(byte[] data, int nalLengthSize) {
        List<ByteBuffer> nalUnits = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.wrap(data);

        while (buf.remaining() > nalLengthSize) {
            int nalLength = readNalLength(buf, nalLengthSize);
            if (nalLength <= 0 || nalLength > buf.remaining()) {
                log.warn("Invalid NAL length {} at position {}, remaining {}",
                        nalLength, buf.position(), buf.remaining());
                break;
            }

            byte[] nalData = new byte[nalLength];
            buf.get(nalData);
            nalUnits.add(ByteBuffer.wrap(nalData));
        }

        return nalUnits;
    }

    private int readNalLength(ByteBuffer buf, int nalLengthSize) {
        int length = 0;
        for (int i = 0; i < nalLengthSize; i++) {
            length = (length << 8) | (buf.get() & 0xFF);
        }
        return length;
    }

    private record AvcCData(List<ByteBuffer> spsList, List<ByteBuffer> ppsList) {}
}
