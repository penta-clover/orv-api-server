package com.orv.api.domain.archive;

import com.orv.api.domain.archive.dto.ConfirmUploadRequest;
import com.orv.api.domain.archive.dto.PresignedUrlResponse;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/archive")
@RequiredArgsConstructor
@Slf4j
public class ArchiveControllerV1 {
    private final ArchiveService archiveService;

    @GetMapping("/upload-url")
    public ApiResponse getUploadUrl(@RequestParam String storyboardId) {
        try {
            String memberId = SecurityContextHolder.getContext().getAuthentication().getName();

            log.info("Requesting upload URL for storyboard: {} by member: {}", storyboardId, memberId);

            PresignedUrlResponse response = archiveService.requestUploadUrl(
                    UUID.fromString(storyboardId),
                    UUID.fromString(memberId)
            );

            return ApiResponse.success(response, 200);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid storyboardId format: {}", storyboardId);
            return ApiResponse.fail(ErrorCode.UNKNOWN, 400);
        } catch (Exception e) {
            log.error("Failed to generate presigned URL", e);
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }

    @PostMapping("/recorded-video")
    public ApiResponse confirmRecordedVideo(@RequestBody ConfirmUploadRequest request) {
        try {
            String memberId = SecurityContextHolder.getContext().getAuthentication().getName();

            log.info("Confirming upload for video: {} by member: {}", request.getVideoId(), memberId);

            Optional<String> result = archiveService.confirmUpload(
                    UUID.fromString(request.getVideoId()),
                    UUID.fromString(memberId)
            );

            if (result.isEmpty()) {
                return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
            }

            return ApiResponse.success(result.get(), 200);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid videoId format: {}", request.getVideoId());
            return ApiResponse.fail(ErrorCode.UNKNOWN, 400);
        } catch (Exception e) {
            log.error("Failed to confirm upload", e);
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }
}
