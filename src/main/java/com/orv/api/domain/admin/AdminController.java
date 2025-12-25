package com.orv.api.domain.admin;

import com.orv.api.domain.archive.ArchiveService;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v0/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final ArchiveService archiveService;

    @GetMapping("/")
    public ApiResponse checkAdmin() {
        log.info("Admin check endpoint hit");
        return ApiResponse.success("You are an admin!", 200);
    }

    @DeleteMapping("/archive/video/{videoId}")
    public ApiResponse deleteVideo(@PathVariable UUID videoId) {
        log.info("Admin delete video request: {}", videoId);
        boolean deleted = archiveService.deleteVideo(videoId);
        if (deleted) {
            return ApiResponse.success("Video deleted successfully", 200);
        } else {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }
    }
}
