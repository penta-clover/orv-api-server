package com.orv.api.domain.archive;

import com.orv.api.domain.archive.dto.Video;
import com.orv.api.domain.archive.dto.VideoMetadata;
import com.orv.api.domain.archive.dto.VideoMetadataUpdateForm;
import com.orv.api.domain.storyboard.dto.Storyboard;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            String memberId = SecurityContextHolder.getContext().getAuthentication().getName();
            Optional<String> uri = videoRepository.save(video.getInputStream(), new VideoMetadata(UUID.fromString(storyboardId), UUID.fromString(memberId), null, video.getContentType(), video.getSize()));

            if (uri.isEmpty()) {
                log.warn("Failed to save video");
                return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
            }

            return ApiResponse.success(uri.get(), 201);
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
                boolean isSuccessful = videoRepository.updateTitle(videoId, updateForm.getTitle());

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
}
