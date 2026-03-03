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

@Component
@Slf4j
public class H264FrameDecoder implements FrameDecoder {

    @Override
    public boolean supports(String codecType) {
        return "avc1".equals(codecType) || "avc3".equals(codecType);
    }

    @Override
    public BufferedImage decode(byte[] codecConfig, byte[] sampleData, int nalLengthSize, int width, int height) {
        AvcCData avcC = parseAvcC(codecConfig);

        H264Decoder decoder = new H264Decoder();
        decoder.addSps(avcC.spsList);
        decoder.addPps(avcC.ppsList);

        List<ByteBuffer> nalUnits = splitAvccNalUnits(sampleData, nalLengthSize);

        int alignedWidth = alignTo16(width);
        int alignedHeight = alignTo16(height);
        byte[][] buffer = Picture.create(alignedWidth, alignedHeight, ColorSpace.YUV420).getData();

        Frame frame = decoder.decodeFrameFromNals(nalUnits, buffer);
        if (frame == null) {
            throw new RuntimeException("H264 decoding returned null frame");
        }

        return AWTUtil.toBufferedImage(frame);
    }

    private static int alignTo16(int value) {
        return (value + 15) & ~15;
    }

    private AvcCData parseAvcC(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.position(5); // skip configurationVersion, profile, compatibility, level, lengthSizeMinusOne

        int numSps = buf.get() & 0x1F;
        List<ByteBuffer> spsList = new ArrayList<>(numSps);
        for (int i = 0; i < numSps; i++) {
            int spsLength = (buf.get() & 0xFF) << 8 | (buf.get() & 0xFF);
            byte[] sps = new byte[spsLength];
            buf.get(sps);
            spsList.add(ByteBuffer.wrap(sps));
        }

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
