package com.orv.recap.controller;

import com.orv.recap.orchestrator.RecapOrchestrator;
import com.orv.recap.controller.dto.*;
import com.orv.recap.orchestrator.dto.*;
import com.orv.common.dto.ApiResponse;
import com.orv.common.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/reservation/recap")
@RequiredArgsConstructor
@Slf4j
public class RecapController {
    private final RecapOrchestrator recapOrchestrator;

    @PostMapping("/video")
    public ApiResponse reserveRecap(@RequestBody RecapReservationRequest request) {
        try {
            UUID memberId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
            UUID videoId = UUID.fromString(request.getVideoId());
            ZonedDateTime scheduledAt = request.getScheduledAt();

            Optional<RecapReservationResponse> response = recapOrchestrator.reserveRecap(memberId, videoId, scheduledAt);

            if (response.isEmpty()) {
                return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
            }

            return ApiResponse.success(response.get(), 201);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }

    @GetMapping("/{recapReservationId}/result")
    public ApiResponse getRecapResult(@PathVariable UUID recapReservationId) {
        Optional<RecapResultResponse> response = recapOrchestrator.getRecapResult(recapReservationId);

        if (response.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        return ApiResponse.success(response.get(), 200);
    }

    @GetMapping("/{recapReservationId}/audio")
    public ApiResponse getRecapAudio(@PathVariable UUID recapReservationId) {
        Optional<RecapAudioResponse> response = recapOrchestrator.getRecapAudio(recapReservationId);

        if (response.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        return ApiResponse.success(response.get(), 200);
    }
}
