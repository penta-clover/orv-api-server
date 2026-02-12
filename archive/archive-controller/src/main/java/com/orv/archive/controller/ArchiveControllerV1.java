package com.orv.archive.controller;

import com.orv.archive.common.ArchiveErrorCode;
import com.orv.archive.common.ArchiveException;
import com.orv.archive.orchestrator.ArchiveOrchestrator;
import com.orv.archive.controller.dto.ConfirmUploadRequest;
import com.orv.archive.controller.dto.SelectThumbnailRequest;
import com.orv.archive.orchestrator.dto.PresignedUrlResponse;
import com.orv.archive.orchestrator.dto.ThumbnailCandidateResponse;
import com.orv.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
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

    @GetMapping("/video/{videoId}/thumbnail-candidates")
    public ApiResponse<?> getThumbnailCandidates(@PathVariable String videoId) {
        UUID videoUuid = parseVideoId(videoId);
        List<ThumbnailCandidateResponse> candidates = archiveOrchestrator.getThumbnailCandidates(videoUuid);
        return ApiResponse.success(candidates, 200);
    }

    @PutMapping("/video/{videoId}/thumbnail/select")
    public ApiResponse<?> selectThumbnail(
            @PathVariable String videoId,
            @Valid @RequestBody SelectThumbnailRequest request) {
        UUID videoUuid = parseVideoId(videoId);
        archiveOrchestrator.selectThumbnailCandidate(videoUuid, request.getCandidateId());
        return ApiResponse.success(null, 200);
    }

    @PutMapping("/video/{videoId}/thumbnail/upload")
    public ApiResponse<?> uploadThumbnail(
            @PathVariable String videoId,
            @RequestPart("thumbnail") MultipartFile thumbnail) throws IOException {
        UUID videoUuid = parseVideoId(videoId);
        boolean result = archiveOrchestrator.updateVideoThumbnail(
                videoUuid, thumbnail.getInputStream(), thumbnail.getContentType(), thumbnail.getSize());
        return ApiResponse.success(result, 200);
    }

    private UUID parseVideoId(String videoId) {
        try {
            return UUID.fromString(videoId);
        } catch (IllegalArgumentException e) {
            throw new ArchiveException(ArchiveErrorCode.INVALID_VIDEO_ID_FORMAT);
        }
    }
}
