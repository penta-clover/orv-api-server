package com.orv.api.domain.archive;

import com.orv.api.domain.archive.dto.ImageMetadata;
import com.orv.api.domain.archive.dto.Video;
import com.orv.api.domain.archive.dto.VideoMetadata;
import com.orv.api.domain.archive.dto.VideoMetadataUpdateForm;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/archive")
@RequiredArgsConstructor
@Slf4j
public class ArchiveController {
    private final VideoRepository videoRepository;

    @PostMapping("/recorded-video")
    public ApiResponse uploadRecordedVideo(@RequestParam("video") MultipartFile video, @RequestParam("storyboardId") String storyboardId) {
        try {
            log.warn("storyboardId: {}", storyboardId);

            String memberId = SecurityContextHolder.getContext().getAuthentication().getName();
            Optional<String> videoId = videoRepository.save(
                    video.getInputStream(),
                    new VideoMetadata(
                            UUID.fromString(storyboardId),
                            UUID.fromString(memberId),
                            null,
                            video.getContentType(),
                            calculateRunningTime(video).intValue(),
                            video.getSize()
                    )
            );

            if (videoId.isEmpty()) {
                log.warn("Failed to save video");
                return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
            }

            return ApiResponse.success(videoId.get(), 201);
        } catch (IOException e) {
            e.printStackTrace();
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }


    @GetMapping("/video/{videoId}")
    public ApiResponse getVideo(@PathVariable String videoId) {
        Optional<Video> foundVideo = videoRepository.findById(UUID.fromString(videoId));

        if (foundVideo.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        Video video = foundVideo.get();
        return ApiResponse.success(video, 200);
    }

    @GetMapping("/videos/my")
    public ApiResponse getMyVideos() {
        String memberId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Video> videos = videoRepository.findByMemberId(UUID.fromString(memberId), 0, 30);
        return ApiResponse.success(videos, 200);
    }

    @PatchMapping("/video/{videoId}")
    public ApiResponse changeVideoMetadata(@PathVariable("videoId") String videoId, @RequestBody VideoMetadataUpdateForm updateForm) {
        try {
            if (updateForm.getTitle() != null && !updateForm.getTitle().isEmpty()) {
                boolean isSuccessful = videoRepository.updateTitle(UUID.fromString(videoId), updateForm.getTitle());

                if (!isSuccessful) {
                    return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
                }
            }

            return ApiResponse.success(null, 200);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }

    @PutMapping("/video/{videoId}/thumbnail")
    public ApiResponse changeVideoThumbnail(@PathVariable("videoId") String videoId, @RequestPart("thumbnail") MultipartFile thumbnail) {
        try {
            boolean isSuccessful = videoRepository.updateThumbnail(UUID.fromString(videoId), thumbnail.getInputStream(), new ImageMetadata(thumbnail.getContentType(), thumbnail.getSize()));

            if (!isSuccessful) {
                return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
            }

            return ApiResponse.success(null, 200);
        } catch (IOException e) {
            e.printStackTrace();
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }

    private Double calculateRunningTime(MultipartFile video) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("upload-" + System.currentTimeMillis(), ".tmp");
            video.transferTo(tempFile);

            double durationInSeconds = 0.0;
            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tempFile)) {
                // 웹m 파일임을 명시적으로 설정
                grabber.setFormat("webm");
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
            e.printStackTrace();
            return 0.0;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
