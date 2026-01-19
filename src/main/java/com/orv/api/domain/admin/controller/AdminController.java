package com.orv.api.domain.admin.controller;

import com.orv.api.domain.admin.service.AdminService;
import com.orv.api.domain.archive.service.dto.Video;
import com.orv.api.domain.auth.service.dto.Member;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/")
    public ApiResponse checkAdmin() {
        log.info("Admin check endpoint hit");
        return ApiResponse.success("You are an admin!", 200);
    }

    @DeleteMapping("/archive/video/{videoId}")
    public ApiResponse deleteVideo(@PathVariable UUID videoId) {
        log.info("Admin delete video request: {}", videoId);
        boolean deleted = adminService.deleteVideo(videoId);
        if (deleted) {
            return ApiResponse.success("Video deleted successfully", 200);
        } else {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }
    }

    @GetMapping("/members")
    public ApiResponse getMembersByProvider(@RequestParam String provider) {
        log.info("Admin get members by provider request: {}", provider);
        List<Member> members = adminService.getMembersByProvider(provider);
        return ApiResponse.success(members, 200);
    }

    @GetMapping("/archive/videos")
    public ApiResponse getVideosByMemberId(@RequestParam UUID memberId) {
        log.info("Admin get videos by member id request: {}", memberId);
        List<Video> videos = adminService.getVideosByMemberId(memberId);
        return ApiResponse.success(videos, 200);
    }
}
