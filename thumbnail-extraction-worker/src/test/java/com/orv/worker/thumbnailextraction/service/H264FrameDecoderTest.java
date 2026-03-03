package com.orv.worker.thumbnailextraction.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class H264FrameDecoderTest {

    private final H264FrameDecoder decoder = new H264FrameDecoder();

    // ── supports ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("supports")
    class SupportsTest {

        @Test
        @DisplayName("avc1 코덱이면 true를 반환한다")
        void supports_avc1Codec_returnsTrue() {
            assertThat(decoder.supports("avc1")).isTrue();
        }

        @Test
        @DisplayName("avc3 코덱이면 true를 반환한다")
        void supports_avc3Codec_returnsTrue() {
            assertThat(decoder.supports("avc3")).isTrue();
        }

        @Test
        @DisplayName("hev1 코덱이면 false를 반환한다")
        void supports_hev1Codec_returnsFalse() {
            assertThat(decoder.supports("hev1")).isFalse();
        }

        @Test
        @DisplayName("mp4a 코덱이면 false를 반환한다")
        void supports_mp4aCodec_returnsFalse() {
            assertThat(decoder.supports("mp4a")).isFalse();
        }

        @Test
        @DisplayName("null이 입력되면 false를 반환한다")
        void supports_nullCodec_returnsFalse() {
            assertThat(decoder.supports(null)).isFalse();
        }
    }

    // ── parseAvcC ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("parseAvcC")
    class ParseAvcCTest {

        @Test
        @DisplayName("SPS 1개와 PPS 1개가 정확히 추출된다")
        void parseAvcC_singleSpsPps_extractsCorrectly() throws Exception {
            byte[] sps = {0x67, 0x42, 0x00, 0x1E, (byte) 0x9A, 0x74, 0x04, 0x01};
            byte[] pps = {0x68, (byte) 0xCE, 0x38, (byte) 0x80};

            byte[] avcCData = buildAvcCBytes(sps, pps, 3);

            Method parseAvcC = H264FrameDecoder.class.getDeclaredMethod("parseAvcC", byte[].class);
            parseAvcC.setAccessible(true);
            Object result = parseAvcC.invoke(decoder, avcCData);

            Method spsListMethod = result.getClass().getDeclaredMethod("spsList");
            Method ppsListMethod = result.getClass().getDeclaredMethod("ppsList");
            spsListMethod.setAccessible(true);
            ppsListMethod.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<ByteBuffer> spsList = (List<ByteBuffer>) spsListMethod.invoke(result);
            @SuppressWarnings("unchecked")
            List<ByteBuffer> ppsList = (List<ByteBuffer>) ppsListMethod.invoke(result);

            assertThat(spsList).hasSize(1);
            assertThat(ppsList).hasSize(1);

            byte[] extractedSps = new byte[spsList.get(0).remaining()];
            spsList.get(0).get(extractedSps);
            assertThat(extractedSps).isEqualTo(sps);

            byte[] extractedPps = new byte[ppsList.get(0).remaining()];
            ppsList.get(0).get(extractedPps);
            assertThat(extractedPps).isEqualTo(pps);
        }
    }

    // ── splitAvccNalUnits ───────────────────────────────────────────────

    @Nested
    @DisplayName("splitAvccNalUnits")
    class SplitAvccNalUnitsTest {

        @Test
        @DisplayName("nalLengthSize가 4이면 NAL unit이 정확히 분리된다")
        void splitAvccNalUnits_nalLengthSize4_splitsCorrectly() throws Exception {
            // NAL 1: length=5, data={0x65, 0x01, 0x02, 0x03, 0x04}
            // NAL 2: length=3, data={0x41, 0x9A, 0x24}
            byte[] avccData = buildAvccSampleData(4,
                    new byte[]{0x65, 0x01, 0x02, 0x03, 0x04},
                    new byte[]{0x41, (byte) 0x9A, 0x24}
            );

            @SuppressWarnings("unchecked")
            List<ByteBuffer> nalUnits = invokeSplitAvccNalUnits(avccData, 4);

            assertThat(nalUnits).hasSize(2);

            byte[] nal1 = new byte[nalUnits.get(0).remaining()];
            nalUnits.get(0).get(nal1);
            assertThat(nal1).isEqualTo(new byte[]{0x65, 0x01, 0x02, 0x03, 0x04});

            byte[] nal2 = new byte[nalUnits.get(1).remaining()];
            nalUnits.get(1).get(nal2);
            assertThat(nal2).isEqualTo(new byte[]{0x41, (byte) 0x9A, 0x24});
        }

        @Test
        @DisplayName("nalLengthSize가 2이면 NAL unit이 정확히 분리된다")
        void splitAvccNalUnits_nalLengthSize2_splitsCorrectly() throws Exception {
            byte[] avccData = buildAvccSampleData(2,
                    new byte[]{0x65, (byte) 0x88}
            );

            @SuppressWarnings("unchecked")
            List<ByteBuffer> nalUnits = invokeSplitAvccNalUnits(avccData, 2);

            assertThat(nalUnits).hasSize(1);
            byte[] nal = new byte[nalUnits.get(0).remaining()];
            nalUnits.get(0).get(nal);
            assertThat(nal).isEqualTo(new byte[]{0x65, (byte) 0x88});
        }

        @Test
        @DisplayName("NAL length가 남은 데이터보다 크면 유효한 NAL만 반환된다")
        void splitAvccNalUnits_invalidNalLength_returnsPartialAndLogsWarning() throws Exception {
            byte[] avccData = concat(
                    new byte[]{0, 0, 0, 2, 0x65, 0x01},  // NAL 1: OK
                    new byte[]{0, 0, 0, 100, 0x00}         // NAL 2: length=100, remaining=1
            );

            Logger logger = (Logger) LoggerFactory.getLogger(H264FrameDecoder.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);

            try {
                @SuppressWarnings("unchecked")
                List<ByteBuffer> nalUnits = invokeSplitAvccNalUnits(avccData, 4);

                assertThat(nalUnits).hasSize(1);
                assertThat(appender.list)
                        .anyMatch(event ->
                                event.getLevel() == Level.WARN
                                        && event.getFormattedMessage().contains("Invalid NAL length"));
            } finally {
                logger.detachAppender(appender);
            }
        }
    }

    // ── 헬퍼 메서드 ──────────────────────────────────────────────────

    private static byte[] buildAvcCBytes(byte[] sps, byte[] pps, int nalLengthSizeMinusOne) {
        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.put((byte) 1);
        buf.put(sps.length > 1 ? sps[1] : 0x42);
        buf.put(sps.length > 2 ? sps[2] : 0x00);
        buf.put(sps.length > 3 ? sps[3] : 0x1E);
        buf.put((byte) (0xFC | nalLengthSizeMinusOne));

        buf.put((byte) (0xE0 | 1));
        buf.putShort((short) sps.length);
        buf.put(sps);

        buf.put((byte) 1);
        buf.putShort((short) pps.length);
        buf.put(pps);

        byte[] result = new byte[buf.position()];
        buf.flip();
        buf.get(result);
        return result;
    }

    private static byte[] buildAvccSampleData(int nalLengthSize, byte[]... nalUnits) {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        for (byte[] nal : nalUnits) {
            for (int i = nalLengthSize - 1; i >= 0; i--) {
                buf.put((byte) (nal.length >> (i * 8)));
            }
            buf.put(nal);
        }
        byte[] result = new byte[buf.position()];
        buf.flip();
        buf.get(result);
        return result;
    }

    private static byte[] concat(byte[]... arrays) {
        int len = 0;
        for (byte[] a : arrays) len += a.length;
        byte[] result = new byte[len];
        int pos = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, pos, a.length);
            pos += a.length;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<ByteBuffer> invokeSplitAvccNalUnits(byte[] data, int nalLengthSize) throws Exception {
        Method method = H264FrameDecoder.class.getDeclaredMethod("splitAvccNalUnits", byte[].class, int.class);
        method.setAccessible(true);
        return (List<ByteBuffer>) method.invoke(decoder, data, nalLengthSize);
    }
}
