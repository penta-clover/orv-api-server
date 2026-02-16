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
        @DisplayName("avc1 코덱 지원")
        void supportsAvc1() {
            assertThat(decoder.supports("avc1")).isTrue();
        }

        @Test
        @DisplayName("avc3 코덱 지원")
        void supportsAvc3() {
            assertThat(decoder.supports("avc3")).isTrue();
        }

        @Test
        @DisplayName("hev1 코덱 미지원")
        void doesNotSupportHev1() {
            assertThat(decoder.supports("hev1")).isFalse();
        }

        @Test
        @DisplayName("mp4a 코덱 미지원")
        void doesNotSupportMp4a() {
            assertThat(decoder.supports("mp4a")).isFalse();
        }

        @Test
        @DisplayName("null 입력 시 false 반환")
        void doesNotSupportNull() {
            assertThat(decoder.supports(null)).isFalse();
        }
    }

    // ── parseAvcC (리플렉션) ─────────────────────────────────────────

    @Nested
    @DisplayName("parseAvcC")
    class ParseAvcCTest {

        @Test
        @DisplayName("SPS 1개와 PPS 1개를 정확히 추출")
        void parseSingleSpsPps() throws Exception {
            byte[] sps = {0x67, 0x42, 0x00, 0x1E, (byte) 0x9A, 0x74, 0x04, 0x01};
            byte[] pps = {0x68, (byte) 0xCE, 0x38, (byte) 0x80};

            byte[] avcCData = buildAvcCBytes(sps, pps, 3);

            // 리플렉션으로 private parseAvcC 호출
            Method parseAvcC = H264FrameDecoder.class.getDeclaredMethod("parseAvcC", byte[].class);
            parseAvcC.setAccessible(true);
            Object result = parseAvcC.invoke(decoder, avcCData);

            // AvcCData record에서 spsList, ppsList 추출
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

            // SPS 내용 검증
            byte[] extractedSps = new byte[spsList.get(0).remaining()];
            spsList.get(0).get(extractedSps);
            assertThat(extractedSps).isEqualTo(sps);

            // PPS 내용 검증
            byte[] extractedPps = new byte[ppsList.get(0).remaining()];
            ppsList.get(0).get(extractedPps);
            assertThat(extractedPps).isEqualTo(pps);
        }
    }

    // ── splitAvccNalUnits (리플렉션) ─────────────────────────────────

    @Nested
    @DisplayName("splitAvccNalUnits")
    class SplitAvccNalUnitsTest {

        @Test
        @DisplayName("nalLengthSize=4인 AVCC 데이터에서 NAL unit 정확 분리")
        void splitWithLength4() throws Exception {
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
        @DisplayName("nalLengthSize=2인 AVCC 데이터에서 NAL unit 정확 분리")
        void splitWithLength2() throws Exception {
            byte[] avccData = buildAvccSampleData(2,
                    new byte[]{0x65, (byte) 0x88}
            );

            @SuppressWarnings("unchecked")
            List<ByteBuffer> nalUnits = invokeSplitAvccNalUnits(avccData, 2);

            assertThat(nalUnits).hasSize(1);
            byte[] nal = new byte[nalUnits.get(0).remaining()];
            nalUnits.get(0).get(nal);
            assertThat(nal).isEqualTo(new byte[]{0x65, (byte) (byte) 0x88});
        }

        @Test
        @DisplayName("NAL length가 남은 데이터보다 크면 warn 로그와 함께 기존 NAL만 반환")
        void invalidNalLengthLogsWarning() throws Exception {
            // 첫 NAL은 정상 (length=2, data=2바이트)
            // 두 번째 NAL은 length=100 (남은 데이터 1바이트보다 큼)
            byte[] avccData = concat(
                    new byte[]{0, 0, 0, 2, 0x65, 0x01},  // NAL 1: OK
                    new byte[]{0, 0, 0, 100, 0x00}         // NAL 2: length=100, remaining=1
            );

            // 로그 캡처 설정
            Logger logger = (Logger) LoggerFactory.getLogger(H264FrameDecoder.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);

            try {
                @SuppressWarnings("unchecked")
                List<ByteBuffer> nalUnits = invokeSplitAvccNalUnits(avccData, 4);

                // 첫 번째 NAL만 반환되어야 함
                assertThat(nalUnits).hasSize(1);

                // WARN 로그 발생 확인
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

    /**
     * avcC 바이트 생성 (테스트용)
     */
    private static byte[] buildAvcCBytes(byte[] sps, byte[] pps, int nalLengthSizeMinusOne) {
        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.put((byte) 1);                                  // configurationVersion
        buf.put(sps.length > 1 ? sps[1] : 0x42);            // AVCProfileIndication
        buf.put(sps.length > 2 ? sps[2] : 0x00);            // profile_compatibility
        buf.put(sps.length > 3 ? sps[3] : 0x1E);            // AVCLevelIndication
        buf.put((byte) (0xFC | nalLengthSizeMinusOne));      // lengthSizeMinusOne

        buf.put((byte) (0xE0 | 1));                          // numSps
        buf.putShort((short) sps.length);
        buf.put(sps);

        buf.put((byte) 1);                                   // numPps
        buf.putShort((short) pps.length);
        buf.put(pps);

        byte[] result = new byte[buf.position()];
        buf.flip();
        buf.get(result);
        return result;
    }

    /**
     * AVCC 포맷 sample 데이터 생성 (nalLengthSize 바이트 길이 prefix + NAL data 반복)
     */
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
