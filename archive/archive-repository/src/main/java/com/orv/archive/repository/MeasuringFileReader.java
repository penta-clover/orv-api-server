package com.orv.archive.repository;

/**
 * VideoFileReader 데코레이터. getRange 호출마다 수신 바이트, 소요 시간, 요청 횟수를 누적한다.
 */
public class MeasuringFileReader implements VideoFileReader {

    private final VideoFileReader delegate;
    private long totalBytes;
    private long totalTimeNs;
    private int requestCount;

    public MeasuringFileReader(VideoFileReader delegate) {
        this.delegate = delegate;
    }

    @Override
    public long getFileSize(String fileKey) {
        return delegate.getFileSize(fileKey);
    }

    @Override
    public byte[] getRange(String fileKey, long offset, long length) {
        long start = System.nanoTime();
        byte[] data = delegate.getRange(fileKey, offset, length);
        totalTimeNs += System.nanoTime() - start;
        totalBytes += data.length;
        requestCount++;
        return data;
    }

    public long totalBytes() { return totalBytes; }

    public long totalTimeMs() { return totalTimeNs / 1_000_000; }

    public int requestCount() { return requestCount; }
}
