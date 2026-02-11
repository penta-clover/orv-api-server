package com.orv.archive.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orv.archive.common.ArchiveErrorCode;
import com.orv.archive.common.ArchiveException;
import com.orv.archive.domain.ImageMetadata;
import com.orv.archive.domain.InputStreamWithMetadata;
import com.orv.archive.domain.PresignedUrlInfo;
import com.orv.archive.domain.Video;
import com.orv.archive.domain.VideoMetadata;
import com.orv.archive.domain.VideoStatus;
import com.orv.archive.repository.VideoDurationCalculationJobRepository;
import com.orv.archive.repository.VideoRepository;
import com.orv.archive.repository.VideoThumbnailExtractionJobRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {
    private static final long PRESIGNED_URL_EXPIRATION_MINUTES = 60;
    private static final String VIDEO_PATH_PREFIX = "archive/videos/";
    private static final String DEFAULT_THUMBNAIL_KEY = "static/images/default-archive-video-thumbnail.png";

    private final VideoRepository videoRepository;
    private final VideoDurationCalculationJobRepository videoDurationCalculationJobRepository;
    private final VideoThumbnailExtractionJobRepository videoThumbnailExtractionJobRepository;

    @Override
    @Deprecated
    public Optional<String> uploadRecordedVideo(InputStream videoStream, String contentType, long size, UUID storyboardId, UUID memberId) {
        File tempFile = null;
        try {
            tempFile = createTempVideoFile(videoStream);
            VideoProcessingResult processingResult = processVideo(tempFile);
            return saveProcessedVideo(tempFile, contentType, size, storyboardId, memberId, processingResult);
        } catch (IOException e) {
            log.error("Failed to upload recorded video", e);
            return Optional.empty();
        } finally {
            deleteTempFile(tempFile);
        }
    }

    @Override
    public Optional<Video> getVideo(UUID videoId) {
        return videoRepository.findById(videoId);
    }

    @Override
    public Optional<InputStream> getVideoStream(UUID videoId) {
        return videoRepository.getVideoStream(videoId);
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

    // v1 API methods below
    @Override
    public PresignedUrlInfo requestUploadUrl(UUID storyboardId, UUID memberId) {
        String videoId = videoRepository.createPendingVideo(storyboardId, memberId);

        URL presignedUrl = videoRepository.generateUploadUrl(UUID.fromString(videoId), PRESIGNED_URL_EXPIRATION_MINUTES);

        Instant expiresAt = Instant.now().plusSeconds(PRESIGNED_URL_EXPIRATION_MINUTES * 60);

        return new PresignedUrlInfo(videoId, presignedUrl.toString(), expiresAt);
    }

    @Override
    @Transactional
    public String confirmUpload(UUID videoId, UUID memberId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> {
                    log.warn("Video not found: {}", videoId);
                    return new ArchiveException(ArchiveErrorCode.VIDEO_NOT_FOUND);
                });

        validateVideoOwnership(video, memberId);
        validateVideoStatus(video, videoId);
        validateUploadComplete(videoId);

        String videoFileKey = VIDEO_PATH_PREFIX + videoId;
        boolean updated = videoRepository.updateVideoFileKeyAndStatus(
                videoId, videoFileKey, VideoStatus.UPLOADED.name());

        if (!updated) {
            log.warn("Failed to update video status: {}", videoId);
            throw new ArchiveException(ArchiveErrorCode.VIDEO_STATUS_UPDATE_FAILED);
        }

        videoRepository.updateThumbnail(videoId, DEFAULT_THUMBNAIL_KEY);

        createProcessingJobs(videoId);

        return videoId.toString();
    }

    @Override
    public boolean deleteVideo(UUID videoId) {
        return videoRepository.deleteVideo(videoId);
    }

    private File createTempVideoFile(InputStream videoStream) throws IOException {
        File tempFile = File.createTempFile("upload-" + System.currentTimeMillis(), ".tmp");
        Files.copy(videoStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }

    private VideoProcessingResult processVideo(File tempFile) throws IOException {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tempFile)) {
            grabber.setFormat("mp4");
            grabber.start();

            Optional<BufferedImage> keyFrame = VideoProcessingUtils.extractKeyFrame(grabber);
            double runningTime = VideoProcessingUtils.calculateRunningTime(grabber);

            grabber.stop();
            return new VideoProcessingResult(keyFrame, runningTime);
        }
    }

    private Optional<String> saveProcessedVideo(File tempFile, String contentType, long size,
                                                 UUID storyboardId, UUID memberId,
                                                 VideoProcessingResult result) throws IOException {
        try (InputStream fileInputStream = Files.newInputStream(tempFile.toPath())) {
            Optional<InputStreamWithMetadata> thumbnailImage = result.getKeyFrame()
                    .map(frame -> {
                        try {
                            return VideoProcessingUtils.bufferedImageToInputStream(frame, "jpg");
                        } catch (IOException e) {
                            log.warn("Failed to convert key frame to input stream", e);
                            return null;
                        }
                    });

            Optional<String> videoId = videoRepository.save(
                    fileInputStream,
                    new VideoMetadata(storyboardId, memberId, null, contentType, (int) result.getRunningTime(), size),
                    thumbnailImage
            );

            if (videoId.isEmpty()) {
                log.warn("Failed to save video");
            }

            return videoId;
        }
    }

    private void validateVideoOwnership(Video video, UUID memberId) {
        if (!video.getMemberId().equals(memberId)) {
            log.warn("Unauthorized access to video: {} by member: {}", video.getId(), memberId);
            throw new ArchiveException(ArchiveErrorCode.VIDEO_ACCESS_DENIED);
        }
    }

    private void validateVideoStatus(Video video, UUID videoId) {
        if (!VideoStatus.PENDING.name().equals(video.getStatus())) {
            log.warn("Video is not in PENDING status: {} (current: {})", videoId, video.getStatus());
            throw new ArchiveException(ArchiveErrorCode.VIDEO_STATUS_NOT_PENDING);
        }
    }

    private void validateUploadComplete(UUID videoId) {
        if (!videoRepository.checkUploadComplete(videoId)) {
            log.warn("Video file not uploaded: {}", videoId);
            throw new ArchiveException(ArchiveErrorCode.VIDEO_FILE_NOT_UPLOADED);
        }
    }

    private void createProcessingJobs(UUID videoId) {
        videoDurationCalculationJobRepository.create(videoId);
        log.info("Created duration calculation job for video: {}", videoId);

        videoThumbnailExtractionJobRepository.create(videoId);
        log.info("Created thumbnail extraction job for video: {}", videoId);
    }

    private void deleteTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            if (!tempFile.delete()) {
                log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
            }
        }
    }

    @RequiredArgsConstructor
    @Getter
    private static class VideoProcessingResult {
        private final Optional<BufferedImage> keyFrame;
        private final double runningTime;
    }

}
