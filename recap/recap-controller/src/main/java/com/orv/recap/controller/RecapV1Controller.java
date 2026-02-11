package com.orv.recap.controller;

import com.orv.recap.orchestrator.RecapOrchestrator;
import com.orv.recap.controller.dto.RecapReservationRequest;
import com.orv.recap.orchestrator.dto.RecapReservationResponse;
import com.orv.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservation/recap")
@RequiredArgsConstructor
@Slf4j
public class RecapV1Controller {
    private final RecapOrchestrator recapOrchestrator;

    @PostMapping("/video")
    public ApiResponse reserveRecap(@RequestBody RecapReservationRequest request) {
        UUID memberId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        UUID videoId = UUID.fromString(request.getVideoId());
        ZonedDateTime scheduledAt = request.getScheduledAt();

        RecapReservationResponse response = recapOrchestrator.reserveRecapAsync(memberId, videoId, scheduledAt);
        return ApiResponse.success(response, 202);
    }
}
