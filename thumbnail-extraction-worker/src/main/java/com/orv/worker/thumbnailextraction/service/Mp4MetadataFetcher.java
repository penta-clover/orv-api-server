package com.orv.worker.thumbnailextraction.service;

import com.orv.archive.repository.VideoFileReader;
import com.orv.archive.service.infrastructure.mp4.Mp4BoxHeader;
import com.orv.archive.service.infrastructure.mp4.Mp4ParseException;
import com.orv.archive.service.infrastructure.mp4.Mp4Parser;
import com.orv.archive.service.infrastructure.mp4.VideoTrackInfo;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class Mp4MetadataFetcher {

    private static final int INITIAL_PROBE_SIZE = 4096;

    public VideoTrackInfo fetch(VideoFileReader reader, String fileKey) {
        long fileSize = reader.getFileSize(fileKey);
        byte[] probeData = reader.getRange(fileKey, 0, Math.min(INITIAL_PROBE_SIZE, fileSize));

        byte[] moovData = fetchMoovData(reader, fileKey, probeData, fileSize);
        return Mp4Parser.parseVideoTrack(moovData);
    }

    private byte[] fetchMoovData(VideoFileReader reader, String fileKey,
                                 byte[] probeData, long fileSize) {
        Optional<Mp4BoxHeader> moovHeader = Mp4Parser.findMoovBox(probeData);

        if (moovHeader.isPresent()) {
            return fetchMoovFromProbe(reader, fileKey, probeData, moovHeader.get());
        }
        return fetchMoovAfterMdat(reader, fileKey, probeData, fileSize);
    }

    private byte[] fetchMoovFromProbe(VideoFileReader reader, String fileKey,
                                      byte[] probeData, Mp4BoxHeader moov) {
        boolean alreadyInProbeData =
                moov.offset() < INITIAL_PROBE_SIZE
                && moov.offset() + moov.totalSize() <= probeData.length;

        if (alreadyInProbeData) {
            byte[] moovData = new byte[(int) moov.totalSize()];
            System.arraycopy(probeData, (int) moov.offset(), moovData, 0, moovData.length);
            return moovData;
        }

        return reader.getRange(fileKey, moov.offset(), moov.totalSize());
    }

    private byte[] fetchMoovAfterMdat(VideoFileReader reader, String fileKey,
                                      byte[] probeData, long fileSize) {
        long inferredOffset = Mp4Parser.inferMoovOffsetAfterMdat(probeData, fileSize);

        byte[] moovHeaderBytes = reader.getRange(fileKey, inferredOffset, 16);
        Mp4BoxHeader verifiedHeader = Mp4Parser.readBoxHeader(moovHeaderBytes, 0);

        if (!"moov".equals(verifiedHeader.type())) {
            throw new Mp4ParseException(
                    "Expected moov at offset %d but found '%s'".formatted(
                            inferredOffset, verifiedHeader.type()));
        }

        return reader.getRange(fileKey, inferredOffset, verifiedHeader.totalSize());
    }
}
