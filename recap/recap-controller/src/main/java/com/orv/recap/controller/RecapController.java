package com.orv.recap.controller;

import com.orv.recap.orchestrator.RecapOrchestrator;
import com.orv.recap.controller.dto.*;
import com.orv.recap.orchestrator.dto.*;
import com.orv.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/reservation/recap")
@RequiredArgsConstructor
@Slf4j
public class RecapController {
    private final RecapOrchestrator recapOrchestrator;

    @PostMapping("/video")
    public ApiResponse reserveRecap(@RequestBody RecapReservationRequest request) {
        UUID memberId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        UUID videoId = UUID.fromString(request.getVideoId());
        ZonedDateTime scheduledAt = request.getScheduledAt();

        RecapReservationResponse response = recapOrchestrator.reserveRecap(memberId, videoId, scheduledAt);
        return ApiResponse.success(response, 201);
    }

    @GetMapping("/{recapReservationId}/result")
    public ApiResponse getRecapResult(@PathVariable UUID recapReservationId) {
        RecapResultResponse response = recapOrchestrator.getRecapResult(recapReservationId);
        return ApiResponse.success(response, 200);
    }

    @GetMapping("/{recapReservationId}/audio")
    public ApiResponse getRecapAudio(@PathVariable UUID recapReservationId) {
        RecapAudioResponse response = recapOrchestrator.getRecapAudio(recapReservationId);
        return ApiResponse.success(response, 200);
    }
}
