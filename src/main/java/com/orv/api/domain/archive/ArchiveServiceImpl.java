package com.orv.api.domain.archive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.springframework.stereotype.Service;

import com.orv.api.domain.archive.dto.ImageMetadata;
import com.orv.api.domain.archive.dto.Video;
import com.orv.api.domain.archive.dto.VideoMetadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveServiceImpl implements ArchiveService {
    private final VideoRepository videoRepository;

    @Override
    public Optional<String> uploadRecordedVideo(InputStream videoStream, String contentType, long size, UUID storyboardId, UUID memberId) {
        try {
            // TODO: 영상 처리 작업을 별도 서버로 분리한 후 아래의 영상 처리 작업 코드를 제거해야 함.
            // Create a temporary file to calculate running time
            File tempFile = File.createTempFile("upload-" + System.currentTimeMillis(), ".tmp");
            try {
                // Copy stream to temp file
                java.nio.file.Files.copy(videoStream, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                // Calculate running time
                double runningTime = calculateRunningTime(tempFile);
                
                // Read the file back as InputStream for repository
                try (InputStream fileInputStream = java.nio.file.Files.newInputStream(tempFile.toPath())) {
                    Optional<String> videoId = videoRepository.save(
                            fileInputStream,
                            new VideoMetadata(
                                    storyboardId,
                                    memberId,
                                    null,
                                    contentType,
                                    (int) runningTime,
                                    size
                            )
                    );
                    
                    if (videoId.isEmpty()) {
                        log.warn("Failed to save video");
                    }
                    
                    return videoId;
                }
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        } catch (IOException e) {
            log.error("Failed to upload recorded video", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Video> getVideo(UUID videoId) {
        return videoRepository.findById(videoId);
    }

    @Override
    public List<Video> getMyVideos(UUID memberId, int page, int pageSize) {
        int offset = page * pageSize;
        return videoRepository.findByMemberId(memberId, offset, pageSize);
    }

    @Override
    public boolean updateVideoTitle(UUID videoId, String title) {
        return videoRepository.updateTitle(videoId, title);
    }

    @Override
    public boolean updateVideoThumbnail(UUID videoId, InputStream thumbnailStream, ImageMetadata metadata) {
        return videoRepository.updateThumbnail(videoId, thumbnailStream, metadata);
    }

    private double calculateRunningTime(File videoFile) {
        // TODO: Running Time 계산 로직을 별도 서버로 분리해야 함.
        try {
            double durationInSeconds = 0.0;
            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
                // mp4 파일임을 명시적으로 설정
                grabber.setFormat("mp4");
                grabber.start();

                long lengthInTime = grabber.getLengthInTime();
                if (lengthInTime > 0) {
                    // 메타데이터에 duration 정보가 있을 경우 사용
                    durationInSeconds = lengthInTime / 1_000_000.0;
                } else {
                    // 메타데이터가 없을 경우, 첫 프레임과 마지막 프레임의 timestamp를 이용해 계산
                    Frame frame;
                    long firstTimestamp = -1;
                    long lastTimestamp = -1;
                    while ((frame = grabber.grabFrame()) != null) {
                        if (frame.timestamp > 0) {
                            if (firstTimestamp == -1) {
                                firstTimestamp = frame.timestamp;
                            }
                            lastTimestamp = frame.timestamp;
                        }
                    }
                    if (firstTimestamp != -1 && lastTimestamp != -1) {
                        durationInSeconds = (lastTimestamp - firstTimestamp) / 1_000_000.0;
                    }
                }
                grabber.stop();
            }
            return durationInSeconds;
        } catch (Exception e) {
            log.error("Failed to calculate running time", e);
            return 0.0;
        }
    }
}
