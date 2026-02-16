package com.orv.archive.service.infrastructure.mp4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 테스트용 MP4 바이트 구조 빌더.
 * ISO 14496-12 스펙에 맞는 MP4 box들을 프로그래밍 방식으로 생성한다.
 */
final class Mp4TestHelper {

    private Mp4TestHelper() {}

    // ── 기본 바이트 유틸리티 ─────────────────────────────────────────

    static byte[] uint32(long value) {
        return new byte[]{
                (byte) (value >> 24), (byte) (value >> 16),
                (byte) (value >> 8), (byte) value
        };
    }

    static byte[] uint16(int value) {
        return new byte[]{(byte) (value >> 8), (byte) value};
    }

    static byte[] int64(long value) {
        return new byte[]{
                (byte) (value >> 56), (byte) (value >> 48),
                (byte) (value >> 40), (byte) (value >> 32),
                (byte) (value >> 24), (byte) (value >> 16),
                (byte) (value >> 8), (byte) value
        };
    }

    static byte[] concat(byte[]... arrays) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] a : arrays) {
                out.write(a);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static byte[] zeros(int count) {
        return new byte[count];
    }

    static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    // ── Box 빌더 ────────────────────────────────────────────────────

    /**
     * 일반 box: [4B size][4B type][children...]
     */
    static byte[] buildBox(String type, byte[]... children) {
        byte[] content = concat(children);
        int totalSize = 8 + content.length;
        return concat(uint32(totalSize), ascii(type), content);
    }

    /**
     * FullBox: [4B size][4B type][1B version][3B flags][content]
     */
    static byte[] buildFullBox(String type, int version, int flags, byte[] content) {
        int totalSize = 8 + 4 + content.length;
        return concat(
                uint32(totalSize), ascii(type),
                new byte[]{(byte) version},
                new byte[]{(byte) (flags >> 16), (byte) (flags >> 8), (byte) flags},
                content
        );
    }

    /**
     * Extended size box: [4B size=1][4B type][8B extended_size][content]
     */
    static byte[] buildExtendedSizeBox(String type, long totalSize, byte[] content) {
        return concat(uint32(1), ascii(type), int64(totalSize), content);
    }

    // ── MP4 고수준 Box 빌더 ─────────────────────────────────────────

    static byte[] buildFtyp() {
        // ftyp: major_brand(4) + minor_version(4) + compatible_brands(...)
        return buildBox("ftyp", ascii("isom"), uint32(0x200), ascii("isomiso2avc1mp41"));
    }

    static byte[] buildMdat(int contentSize) {
        int totalSize = 8 + contentSize;
        return concat(uint32(totalSize), ascii("mdat"), zeros(contentSize));
    }

    /**
     * mdhd version 0: creation_time(4) + modification_time(4) + timescale(4) + duration(4) + lang(2) + pre(2)
     */
    static byte[] buildMdhd(long timescale, long duration) {
        return buildMdhd(0, timescale, duration);
    }

    /**
     * mdhd version 1: creation_time(8) + modification_time(8) + timescale(4) + duration(8) + lang(2) + pre(2)
     */
    static byte[] buildMdhdV1(long timescale, long duration) {
        return buildMdhd(1, timescale, duration);
    }

    private static byte[] buildMdhd(int version, long timescale, long duration) {
        if (version == 0) {
            return buildMdhd0(timescale, duration);
        }
        byte[] content = concat(
                int64(0),              // creation_time
                int64(0),              // modification_time
                uint32(timescale),     // timescale
                int64(duration),       // duration
                uint16(0x55C4),        // language (undetermined)
                uint16(0)              // pre_defined
        );
        return buildFullBox("mdhd", 1, 0, content);
    }

    private static byte[] buildMdhd0(long timescale, long duration) {
        byte[] content = concat(
                uint32(0),             // creation_time
                uint32(0),             // modification_time
                uint32(timescale),     // timescale
                uint32(duration),      // duration
                uint16(0x55C4),        // language (undetermined)
                uint16(0)              // pre_defined
        );
        return buildFullBox("mdhd", 0, 0, content);
    }

    /**
     * hdlr: pre_defined(4) + handler_type(4) + reserved(12) + name(null-terminated)
     */
    static byte[] buildHdlr(String handlerType) {
        byte[] content = concat(
                uint32(0),                          // pre_defined
                ascii(handlerType),                 // handler_type
                zeros(12),                          // reserved
                ascii("VideoHandler"), new byte[]{0} // name (null-terminated)
        );
        return buildFullBox("hdlr", 0, 0, content);
    }

    /**
     * stts FullBox: entry_count(4) + [sample_count(4) + sample_delta(4)]...
     */
    static byte[] buildStts(List<int[]> entries) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(uint32(entries.size()));
            for (int[] entry : entries) {
                out.write(uint32(entry[0])); // sample_count
                out.write(uint32(entry[1])); // sample_delta
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return buildFullBox("stts", 0, 0, out.toByteArray());
    }

    /**
     * stss FullBox: entry_count(4) + [sample_number(4)]...
     */
    static byte[] buildStss(int[] syncSamples) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(uint32(syncSamples.length));
            for (int s : syncSamples) {
                out.write(uint32(s));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return buildFullBox("stss", 0, 0, out.toByteArray());
    }

    /**
     * stsc FullBox: entry_count(4) + [first_chunk(4) + spc(4) + sdi(4)]...
     */
    static byte[] buildStsc(List<int[]> entries) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(uint32(entries.size()));
            for (int[] entry : entries) {
                out.write(uint32(entry[0])); // first_chunk (1-based)
                out.write(uint32(entry[1])); // samples_per_chunk
                out.write(uint32(entry[2])); // sample_description_index
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return buildFullBox("stsc", 0, 0, out.toByteArray());
    }

    /**
     * stsz FullBox: sample_size(4) + sample_count(4) + [entry_size(4)]...
     */
    static byte[] buildStsz(int[] sampleSizes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(uint32(0)); // sample_size (0 = 가변)
            out.write(uint32(sampleSizes.length));
            for (int size : sampleSizes) {
                out.write(uint32(size));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return buildFullBox("stsz", 0, 0, out.toByteArray());
    }

    /**
     * stco FullBox: entry_count(4) + [chunk_offset(4)]...
     */
    static byte[] buildStco(long[] chunkOffsets) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(uint32(chunkOffsets.length));
            for (long offset : chunkOffsets) {
                out.write(uint32(offset));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return buildFullBox("stco", 0, 0, out.toByteArray());
    }

    /**
     * avcC (AVC Decoder Configuration Record):
     * configVersion(1) + profile(1) + compat(1) + level(1) + nalLengthSizeMinusOne(1)
     * + numSps(1) + [spsLen(2) + sps]... + numPps(1) + [ppsLen(2) + pps]...
     */
    static byte[] buildAvcC(byte[] sps, byte[] pps, int nalLengthSizeMinusOne) {
        return concat(
                new byte[]{
                        1,                                          // configurationVersion
                        sps.length > 1 ? sps[1] : 0x42,            // AVCProfileIndication (from SPS)
                        sps.length > 2 ? sps[2] : 0x00,            // profile_compatibility
                        sps.length > 3 ? sps[3] : 0x1E,            // AVCLevelIndication
                        (byte) (0xFC | nalLengthSizeMinusOne)       // 111111xx | nalLengthSizeMinusOne
                },
                // SPS
                new byte[]{(byte) (0xE0 | 1)},                     // 111xxxxx | numSps=1
                uint16(sps.length),
                sps,
                // PPS
                new byte[]{1},                                      // numPps=1
                uint16(pps.length),
                pps
        );
    }

    /**
     * stsd FullBox 내 avc1 entry를 포함하는 stsd 생성.
     * VisualSampleEntry 고정 필드 78바이트 + avcC child box.
     */
    static byte[] buildStsd(int width, int height, byte[] avcCData) {
        // avcC를 box로 감싸기
        byte[] avcCBox = buildBox("avcC", avcCData);

        // VisualSampleEntry 고정 필드 (78 bytes):
        // reserved(6) + data_ref_index(2) + pre_defined(2) + reserved(2) + pre_defined(12)
        // + width(2) + height(2) + horizresolution(4) + vertresolution(4)
        // + reserved(4) + frame_count(2) + compressorname(32) + depth(2) + pre_defined(2)
        byte[] visualSampleEntry = concat(
                zeros(6),                  // reserved
                uint16(1),                 // data_ref_index
                uint16(0), uint16(0),      // pre_defined, reserved
                zeros(12),                 // pre_defined
                uint16(width),
                uint16(height),
                uint32(0x00480000),        // horizresolution (72 dpi)
                uint32(0x00480000),        // vertresolution (72 dpi)
                uint32(0),                 // reserved
                uint16(1),                 // frame_count
                zeros(32),                 // compressorname
                uint16(0x0018),            // depth (24)
                uint16(0xFFFF)             // pre_defined (-1)
        );

        byte[] avc1Box = buildBox("avc1", visualSampleEntry, avcCBox);

        // stsd: version(1) + flags(3) + entry_count(4) + entries...
        byte[] stsdContent = concat(uint32(1), avc1Box); // entry_count = 1
        return buildFullBox("stsd", 0, 0, stsdContent);
    }

    // ── 전체 moov 구조 빌더 ──────────────────────────────────────────

    static byte[] buildVideoMoov(VideoMoovParams p) {
        byte[] avcCData = buildAvcC(p.sps, p.pps, p.nalLengthSizeMinusOne);

        byte[] stbl = buildBox("stbl",
                buildStsd(p.width, p.height, avcCData),
                buildStts(p.sttsEntries),
                buildStss(p.syncSamples),
                buildStsc(p.stscEntries),
                buildStsz(p.sampleSizes),
                buildStco(p.chunkOffsets)
        );

        byte[] minf = buildBox("minf", stbl);
        byte[] hdlr = buildHdlr("vide");
        byte[] mdhd = buildMdhd(p.mdhdVersion, p.timescale, p.durationTicks);
        byte[] mdia = buildBox("mdia", mdhd, hdlr, minf);
        byte[] trak = buildBox("trak", mdia);

        return buildBox("moov", trak);
    }

    /**
     * 오디오 트랙만 있는 moov (비디오 트랙 없음 테스트용)
     */
    static byte[] buildAudioOnlyMoov() {
        byte[] hdlr = buildHdlr("soun");
        byte[] mdhd = buildMdhd(44100, 44100 * 10); // 10초 오디오

        // 최소한의 stbl (stsd는 mp4a 더미)
        byte[] stsdContent = concat(uint32(1), buildBox("mp4a", zeros(28)));
        byte[] stsd = buildFullBox("stsd", 0, 0, stsdContent);
        byte[] stts = buildStts(List.of(new int[]{441000, 1}));
        byte[] stsc = buildStsc(List.of(new int[]{1, 1, 1}));
        byte[] stsz = buildStsz(new int[]{1024});
        byte[] stco = buildStco(new long[]{0});
        byte[] stbl = buildBox("stbl", stsd, stts, stsc, stsz, stco);

        byte[] minf = buildBox("minf", stbl);
        byte[] mdia = buildBox("mdia", mdhd, hdlr, minf);
        byte[] trak = buildBox("trak", mdia);

        return buildBox("moov", trak);
    }

    /**
     * stbl에서 특정 box를 누락시킨 moov 생성 (에러 테스트용)
     */
    static byte[] buildMoovMissingBox(String boxToOmit) {
        byte[] avcCData = buildAvcC(new byte[]{0x67, 0x42, 0x00, 0x1E}, new byte[]{0x68, (byte) 0xCE, 0x38, (byte) 0x80}, 3);

        byte[] stsd = buildStsd(1920, 1080, avcCData);
        byte[] stts = buildStts(List.of(new int[]{30, 1000}));
        byte[] stss = buildStss(new int[]{1});
        byte[] stsc = buildStsc(List.of(new int[]{1, 1, 1}));
        byte[] stsz = buildStsz(new int[]{50000});
        byte[] stco = buildStco(new long[]{1000});

        ByteArrayOutputStream stblContent = new ByteArrayOutputStream();
        try {
            if (!"stsd".equals(boxToOmit)) stblContent.write(stsd);
            if (!"stts".equals(boxToOmit)) stblContent.write(stts);
            if (!"stss".equals(boxToOmit)) stblContent.write(stss);
            if (!"stsc".equals(boxToOmit)) stblContent.write(stsc);
            if (!"stsz".equals(boxToOmit)) stblContent.write(stsz);
            if (!"stco".equals(boxToOmit)) stblContent.write(stco);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] stbl = buildBox("stbl", stblContent.toByteArray());
        byte[] minf = buildBox("minf", stbl);
        byte[] hdlr = buildHdlr("vide");
        byte[] mdhd = buildMdhd(30000, 30000);
        byte[] mdia = buildBox("mdia", mdhd, hdlr, minf);
        byte[] trak = buildBox("trak", mdia);

        return buildBox("moov", trak);
    }

    // ── 파라미터 홀더 ────────────────────────────────────────────────

    static class VideoMoovParams {
        int mdhdVersion = 0;
        long timescale = 30000;
        long durationTicks = 300000; // 10초 (30000 * 10)
        int width = 1920;
        int height = 1080;
        byte[] sps = {0x67, 0x42, 0x00, 0x1E, (byte) 0x9A, 0x74, 0x04, 0x01};
        byte[] pps = {0x68, (byte) 0xCE, 0x38, (byte) 0x80};
        int nalLengthSizeMinusOne = 3; // nalLengthSize = 4
        List<int[]> sttsEntries = List.of(new int[]{10, 1000}); // 10 samples, delta=1000
        int[] syncSamples = {1, 5, 9}; // 1-based
        List<int[]> stscEntries = List.of(
                new int[]{1, 5, 1}, // chunk 1-2: 5 samples per chunk
                new int[]{3, 5, 1}  // chunk 3+: 5 samples per chunk (사실 동일하지만 복수 엔트리 테스트)
        );
        int[] sampleSizes = {50000, 10000, 10000, 10000, 40000, 10000, 10000, 10000, 35000, 10000};
        long[] chunkOffsets = {1000, 81000}; // 2 chunks
    }
}
