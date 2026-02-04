package com.orv.archive.controller;

import com.orv.archive.common.ArchiveErrorCode;
import com.orv.archive.common.ArchiveException;
import com.orv.archive.orchestrator.ArchiveOrchestrator;
import com.orv.archive.controller.dto.ConfirmUploadRequest;
import com.orv.archive.orchestrator.dto.PresignedUrlResponse;
import com.orv.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/archive")
@RequiredArgsConstructor
@Slf4j
public class ArchiveControllerV1 {
    private final ArchiveOrchestrator archiveOrchestrator;

    @GetMapping("/upload-url")
    public ApiResponse<?> getUploadUrl(@RequestParam String storyboardId) {
        String memberId = SecurityContextHolder.getContext().getAuthentication().getName();

        log.info("Requesting upload URL for storyboard: {} by member: {}", storyboardId, memberId);

        UUID storyboardUuid;
        try {
            storyboardUuid = UUID.fromString(storyboardId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid storyboardId format: {}", storyboardId);
            throw new ArchiveException(ArchiveErrorCode.INVALID_STORYBOARD_ID_FORMAT);
        }

        PresignedUrlResponse response = archiveOrchestrator.requestUploadUrl(
                storyboardUuid,
                UUID.fromString(memberId)
        );

        return ApiResponse.success(response, 200);
    }

    @PostMapping("/recorded-video")
    public ApiResponse<?> confirmRecordedVideo(@Valid @RequestBody ConfirmUploadRequest request) {
        String memberId = SecurityContextHolder.getContext().getAuthentication().getName();

        log.info("Confirming upload for video: {} by member: {}", request.getVideoId(), memberId);

        UUID videoUuid;
        try {
            videoUuid = UUID.fromString(request.getVideoId());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("Invalid videoId format: {}", request.getVideoId());
            throw new ArchiveException(ArchiveErrorCode.INVALID_VIDEO_ID_FORMAT);
        }

        String videoId = archiveOrchestrator.confirmUpload(
                videoUuid,
                UUID.fromString(memberId)
        );

        return ApiResponse.success(videoId, 200);
    }
}
