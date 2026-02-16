package com.orv.archive.service.infrastructure.mp4;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 순수 Java 기반 MP4 컨테이너 파서.
 * moov box 위치 탐색, sample table 파싱, 키프레임 바이트 오프셋 계산을 수행한다.
 */
public final class Mp4Parser {

    private Mp4Parser() {}

    /**
     * 프로브 데이터에서 moov box를 찾는다.
     *
     * @param headerData 파일 앞부분의 바이트
     * @return moov를 찾으면 정확한 헤더 정보, 못 찾으면 empty
     */
    public static Optional<Mp4BoxHeader> findMoovBox(byte[] headerData) {
        List<Mp4BoxHeader> boxes = scanBoxHeaders(headerData, 0, headerData.length, 0);

        for (Mp4BoxHeader box : boxes) {
            if ("moov".equals(box.type())) {
                return Optional.of(box);
            }
        }

        return Optional.empty();
    }

    /**
     * 프로브 데이터에서 mdat box를 찾아 그 뒤 오프셋을 반환한다.
     * moov가 프로브 데이터에 없을 때, mdat 뒤에 위치할 것으로 추정하는 용도.
     *
     * @param headerData 파일 앞부분의 바이트
     * @param fileSize   전체 파일 크기
     * @return mdat 끝 오프셋 (moov 추정 위치)
     * @throws Mp4ParseException mdat를 찾을 수 없는 경우
     */
    public static long inferMoovOffsetAfterMdat(byte[] headerData, long fileSize) {
        List<Mp4BoxHeader> boxes = scanBoxHeaders(headerData, 0, headerData.length, 0);

        for (Mp4BoxHeader box : boxes) {
            if ("mdat".equals(box.type())) {
                long moovOffset = box.offset() + box.totalSize();
                if (moovOffset < fileSize) {
                    return moovOffset;
                }
            }
        }

        throw new Mp4ParseException("Cannot locate mdat box to infer moov position");
    }

    // ── moov 파싱 ──────────────────────────────────────────────────────

    /**
     * moov box 바이트를 파싱하여 비디오 트랙 정보를 추출한다.
     *
     * @param moovData moov box 전체 바이트 (헤더 포함)
     * @return 비디오 트랙 정보
     */
    public static VideoTrackInfo parseVideoTrack(byte[] moovData) {
        // moov 헤더 파싱
        Mp4BoxHeader moovHeader = readBoxHeader(moovData, 0);
        int moovDataStart = moovHeader.headerSize();

        // moov 내부 box들 스캔
        List<Mp4BoxHeader> moovChildren = scanBoxHeaders(
                moovData, moovDataStart, (int) moovHeader.totalSize(), 0);

        // trak box들을 찾아 비디오 트랙 식별
        for (Mp4BoxHeader trakHeader : moovChildren) {
            if (!"trak".equals(trakHeader.type())) continue;

            VideoTrackInfo trackInfo = tryParseVideoTrack(moovData, trakHeader);
            if (trackInfo != null) {
                return trackInfo;
            }
        }

        throw new Mp4ParseException("No video track found in moov");
    }

    /**
     * 단일 trak box를 파싱하여 비디오 트랙이면 VideoTrackInfo를 반환한다.
     */
    private static VideoTrackInfo tryParseVideoTrack(byte[] data, Mp4BoxHeader trakHeader) {
        Mp4BoxHeader mdia = findChildBox(data, trakHeader, "mdia");
        if (mdia == null) { return null; }

        Mp4BoxHeader hdlr = findChildBox(data, mdia, "hdlr");
        if (hdlr == null) { return null; }
        
        boolean isNotVideoTrack = !"vide".equals(readHandlerType(data, hdlr));
        if (isNotVideoTrack) { return null; }

        // mdhd box에서 timescale, duration 추출
        Mp4BoxHeader mdhd = requireChildBox(data, mdia, "mdhd");

        long[] timescaleAndDuration = readMdhd(data, mdhd);
        long timescale = timescaleAndDuration[0];
        long durationMs = timescaleAndDuration[1] * 1000 / timescale;

        // minf.stbl 찾기
        Mp4BoxHeader minf = requireChildBox(data, mdia, "minf");
        Mp4BoxHeader stbl = requireChildBox(data, minf, "stbl");

        // stsd에서 코덱 정보 추출
        Mp4BoxHeader stsd = requireChildBox(data, stbl, "stsd");
        CodecInfo codecInfo = parseStsd(data, stsd);

        // sample table 파싱
        SampleTableInfo sampleTable = parseSampleTable(data,
                (int) stbl.dataOffset(), (int) (stbl.offset() + stbl.totalSize()));

        return new VideoTrackInfo(
                timescale, durationMs, codecInfo.width, codecInfo.height, codecInfo.codecType, codecInfo.codecConfig,
                codecInfo.nalLengthSize, sampleTable
        );
    }

    // ── Box 헤더 읽기 ───────────────────────────────────────────────────

    /**
     * 바이트 배열의 특정 위치에서 box 헤더를 읽는다.
     */
    public static Mp4BoxHeader readBoxHeader(byte[] data, int position) {
        if (position + 8 > data.length) {
            throw new Mp4ParseException("Not enough data for box header at position " + position);
        }

        long size = readUint32(data, position);
        String type = readString(data, position + 4, 4);
        int headerSize = 8;

        if (size == 1) {
            // extended size (64-bit)
            if (position + 16 > data.length) {
                throw new Mp4ParseException("Not enough data for extended box header");
            }
            size = readInt64(data, position + 8);
            headerSize = 16;
        } else if (size == 0) {
            // box extends to end of file — 호출자가 처리해야 함
            // 일단 나머지 전체를 크기로 설정
            size = data.length - position;
        }

        return new Mp4BoxHeader(type, position, size, headerSize);
    }

    /**
     * 주어진 범위 내에서 box 헤더들을 순차적으로 스캔한다.
     */
    private static List<Mp4BoxHeader> scanBoxHeaders(byte[] data, int start, int limit, long baseOffset) {
        List<Mp4BoxHeader> headers = new ArrayList<>();
        int pos = start;

        while (pos + 8 <= Math.min(data.length, limit)) {
            Mp4BoxHeader header = readBoxHeader(data, pos);
            if (header.totalSize() < 8) break; // 잘못된 box

            // baseOffset을 적용하여 파일 기준 offset 생성
            if (baseOffset != 0) {
                header = new Mp4BoxHeader(
                        header.type(),
                        header.offset() + baseOffset,
                        header.totalSize(),
                        header.headerSize()
                );
            }

            headers.add(header);
            pos += (int) header.totalSize();
        }

        return headers;
    }

    /**
     * 주어진 범위 내에서 특정 타입의 child box를 찾는다.
     */
    private static Mp4BoxHeader findChildBox(byte[] data, Mp4BoxHeader parent, String type) {
        return findChildBox(data, (int) parent.dataOffset(),
                (int) (parent.offset() + parent.totalSize()), type);
    }

    private static Mp4BoxHeader findChildBox(byte[] data, int start, int end, String type) {
        int pos = start;
        while (pos + 8 <= end) {
            Mp4BoxHeader header = readBoxHeader(data, pos);
            if (header.totalSize() < 8) break;
            if (type.equals(header.type())) return header;
            pos += (int) header.totalSize();
        }
        return null;
    }

    private static Mp4BoxHeader requireChildBox(byte[] data, Mp4BoxHeader parent, String type) {
        Mp4BoxHeader child = findChildBox(data, parent, type);
        if (child == null) {
            throw new Mp4ParseException(type + " box not found");
        }
        return child;
    }

    // ── 특정 box 파싱 ──────────────────────────────────────────────────

    /**
     * hdlr box에서 handler_type을 읽는다.
     * hdlr 구조: version(1) + flags(3) + pre_defined(4) + handler_type(4) + ...
     */
    private static String readHandlerType(byte[] data, Mp4BoxHeader hdlrHeader) {
        int dataStart = (int) hdlrHeader.dataOffset();
        // version(1) + flags(3) = 4바이트 건너뛰기
        // pre_defined(4) 건너뛰기
        return readString(data, dataStart + 8, 4);
    }

    /**
     * mdhd box에서 timescale과 duration을 읽는다.
     * version 0: timescale@offset12, duration@offset16 (uint32)
     * version 1: timescale@offset20, duration@offset24 (uint64 duration)
     */
    private static long[] readMdhd(byte[] data, Mp4BoxHeader mdhdHeader) {
        int dataStart = (int) mdhdHeader.dataOffset();
        int version = data[dataStart] & 0xFF;

        if (version == 0) {
            long timescale = readUint32(data, dataStart + 4 + 8);
            long duration = readUint32(data, dataStart + 4 + 12);
            return new long[]{timescale, duration};
        } else {
            long timescale = readUint32(data, dataStart + 4 + 16);
            long duration = readInt64(data, dataStart + 4 + 20);
            return new long[]{timescale, duration};
        }
    }

    // ── stsd 파싱 (코덱 정보) ───────────────────────────────────────────

    private record CodecInfo(String codecType, int width, int height,
                             byte[] codecConfig, int nalLengthSize) {}

    /**
     * stsd box를 파싱하여 코덱 정보를 추출한다.
     * stsd 구조: version(1) + flags(3) + entry_count(4) + entries...
     * 각 entry는 SampleEntry → VisualSampleEntry → avc1/hev1 등
     */
    private static CodecInfo parseStsd(byte[] data, Mp4BoxHeader stsdHeader) {
        int pos = (int) stsdHeader.dataOffset();
        pos += 4; // version + flags
        int entryCount = (int) readUint32(data, pos);
        pos += 4;

        if (entryCount < 1) {
            throw new Mp4ParseException("stsd has no entries");
        }

        // 첫 번째 entry 읽기
        Mp4BoxHeader entryHeader = readBoxHeader(data, pos);
        String codecType = entryHeader.type();

        // VisualSampleEntry 구조:
        // reserved(6) + data_ref_index(2) + pre_defined(2) + reserved(2) + pre_defined(12)
        // + width(2) + height(2) + ...
        int entryDataStart = (int) entryHeader.dataOffset();
        int width = readUint16(data, entryDataStart + 24);
        int height = readUint16(data, entryDataStart + 26);

        // avc1/hev1 내부에서 avcC/hvcC box 찾기
        // VisualSampleEntry의 고정 필드 후 (78바이트 from entry data start) child box들이 시작
        int childStart = entryDataStart + 78;
        int entryEnd = pos + (int) entryHeader.totalSize();

        byte[] codecConfig = null;
        int nalLengthSize = 4;

        String configBoxType = switch (codecType) {
            case "avc1", "avc3" -> "avcC";
            case "hev1", "hvc1" -> "hvcC";
            default -> null;
        };

        if (configBoxType != null) {
            Mp4BoxHeader configBox = findChildBox(data, childStart, entryEnd, configBoxType);
            if (configBox != null) {
                int configStart = (int) configBox.dataOffset();
                int configSize = (int) configBox.dataSize();
                codecConfig = new byte[configSize];
                System.arraycopy(data, configStart, codecConfig, 0, configSize);

                // avcC의 NAL length size: byte[4] & 0x03 + 1
                if ("avcC".equals(configBoxType) && configSize > 4) {
                    nalLengthSize = (codecConfig[4] & 0x03) + 1;
                }
            }
        }

        return new CodecInfo(codecType, width, height, codecConfig, nalLengthSize);
    }

    // ── Sample Table 파싱 ───────────────────────────────────────────────

    private static SampleTableInfo parseSampleTable(
            byte[] data, int stblStart, int stblEnd) {

        List<SampleTableInfo.SttsEntry> sttsEntries = new ArrayList<>();
        int[] syncSamples = null;
        List<SampleTableInfo.StscEntry> stscEntries = new ArrayList<>();
        int[] sampleSizes = null;
        long[] chunkOffsets = null;

        int pos = stblStart;
        while (pos + 8 <= stblEnd) {
            Mp4BoxHeader box = readBoxHeader(data, pos);
            if (box.totalSize() < 8) break;

            int dataPos = (int) box.dataOffset();

            switch (box.type()) {
                case "stts" -> sttsEntries = parseStts(data, dataPos);
                case "stss" -> syncSamples = parseStss(data, dataPos);
                case "stsc" -> stscEntries = parseStsc(data, dataPos);
                case "stsz" -> sampleSizes = parseStsz(data, dataPos);
                case "stco" -> chunkOffsets = parseStco(data, dataPos);
                case "co64" -> chunkOffsets = parseCo64(data, dataPos);
            }

            pos += (int) box.totalSize();
        }

        if (sttsEntries.isEmpty()) throw new Mp4ParseException("stts not found");
        if (stscEntries.isEmpty()) throw new Mp4ParseException("stsc not found");
        if (sampleSizes == null) throw new Mp4ParseException("stsz not found");
        if (chunkOffsets == null) throw new Mp4ParseException("stco/co64 not found");

        return new SampleTableInfo(sttsEntries, syncSamples, stscEntries, sampleSizes, chunkOffsets);
    }

    /** stts: version(1) + flags(3) + entry_count(4) + entries(sample_count(4) + sample_delta(4)) */
    private static List<SampleTableInfo.SttsEntry> parseStts(byte[] data, int dataPos) {
        int pos = dataPos + 4; // skip version + flags
        int entryCount = (int) readUint32(data, pos);
        pos += 4;

        List<SampleTableInfo.SttsEntry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            int sampleCount = (int) readUint32(data, pos);
            int sampleDelta = (int) readUint32(data, pos + 4);
            entries.add(new SampleTableInfo.SttsEntry(sampleCount, sampleDelta));
            pos += 8;
        }
        return entries;
    }

    /** stss: version(1) + flags(3) + entry_count(4) + sample_number(4)... */
    private static int[] parseStss(byte[] data, int dataPos) {
        int pos = dataPos + 4; // skip version + flags
        int entryCount = (int) readUint32(data, pos);
        pos += 4;

        int[] syncSamples = new int[entryCount];
        for (int i = 0; i < entryCount; i++) {
            syncSamples[i] = (int) readUint32(data, pos);
            pos += 4;
        }
        return syncSamples;
    }

    /** stsc: version(1) + flags(3) + entry_count(4) + entries(first_chunk(4) + spc(4) + sdi(4)) */
    private static List<SampleTableInfo.StscEntry> parseStsc(byte[] data, int dataPos) {
        int pos = dataPos + 4;
        int entryCount = (int) readUint32(data, pos);
        pos += 4;

        List<SampleTableInfo.StscEntry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            int firstChunk = (int) readUint32(data, pos);
            int samplesPerChunk = (int) readUint32(data, pos + 4);
            int sdi = (int) readUint32(data, pos + 8);
            entries.add(new SampleTableInfo.StscEntry(firstChunk, samplesPerChunk, sdi));
            pos += 12;
        }
        return entries;
    }

    /** stsz: version(1) + flags(3) + sample_size(4) + sample_count(4) + entries(4)... */
    private static int[] parseStsz(byte[] data, int dataPos) {
        int pos = dataPos + 4; // skip version + flags
        int defaultSize = (int) readUint32(data, pos);
        int sampleCount = (int) readUint32(data, pos + 4);
        pos += 8;

        int[] sizes = new int[sampleCount];
        if (defaultSize != 0) {
            // 모든 sample이 같은 크기
            for (int i = 0; i < sampleCount; i++) {
                sizes[i] = defaultSize;
            }
        } else {
            for (int i = 0; i < sampleCount; i++) {
                sizes[i] = (int) readUint32(data, pos);
                pos += 4;
            }
        }
        return sizes;
    }

    /** stco: version(1) + flags(3) + entry_count(4) + chunk_offset(4)... */
    private static long[] parseStco(byte[] data, int dataPos) {
        int pos = dataPos + 4;
        int entryCount = (int) readUint32(data, pos);
        pos += 4;

        long[] offsets = new long[entryCount];
        for (int i = 0; i < entryCount; i++) {
            offsets[i] = readUint32(data, pos);
            pos += 4;
        }
        return offsets;
    }

    /** co64: version(1) + flags(3) + entry_count(4) + chunk_offset(8)... */
    private static long[] parseCo64(byte[] data, int dataPos) {
        int pos = dataPos + 4;
        int entryCount = (int) readUint32(data, pos);
        pos += 4;

        long[] offsets = new long[entryCount];
        for (int i = 0; i < entryCount; i++) {
            offsets[i] = readInt64(data, pos);
            pos += 8;
        }
        return offsets;
    }

    // ── 바이트 읽기 유틸리티 ────────────────────────────────────────────

    static long readUint32(byte[] data, int offset) {
        return ((long) (data[offset] & 0xFF) << 24)
                | ((long) (data[offset + 1] & 0xFF) << 16)
                | ((long) (data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    static long readInt64(byte[] data, int offset) {
        return ((long) (data[offset] & 0xFF) << 56)
                | ((long) (data[offset + 1] & 0xFF) << 48)
                | ((long) (data[offset + 2] & 0xFF) << 40)
                | ((long) (data[offset + 3] & 0xFF) << 32)
                | ((long) (data[offset + 4] & 0xFF) << 24)
                | ((long) (data[offset + 5] & 0xFF) << 16)
                | ((long) (data[offset + 6] & 0xFF) << 8)
                | (data[offset + 7] & 0xFF);
    }

    static int readUint16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    static String readString(byte[] data, int offset, int length) {
        return new String(data, offset, length, StandardCharsets.ISO_8859_1);
    }
}
