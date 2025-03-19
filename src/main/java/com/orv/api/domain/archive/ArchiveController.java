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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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

            double durationInSeconds;

            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tempFile)) {
                grabber.start();
                durationInSeconds = grabber.getLengthInTime() / 1_000_000.0;
                grabber.stop();
            }

            return durationInSeconds;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }
}
