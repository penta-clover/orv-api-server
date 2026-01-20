package com.orv.api.domain.reservation.controller;

import com.orv.api.domain.reservation.orchestrator.ReservationOrchestrator;
import com.orv.api.domain.reservation.controller.dto.*;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/reservation")
@RequiredArgsConstructor
@Slf4j
public class ReservationController {
    private final ReservationOrchestrator reservationOrchestrator;

    @PostMapping("/interview")
    public ApiResponse reserveInterview(@RequestBody InterviewReservationRequest request, @RequestParam(value="startNow", required = false, defaultValue = "false") Boolean startNow) {
        try {
            UUID memberId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
            UUID storyboardId = UUID.fromString(request.getStoryboardId());
            OffsetDateTime scheduledAt = request.getReservedAt() != null ? request.getReservedAt().toOffsetDateTime() : OffsetDateTime.now();

            Optional<InterviewReservationResponse> response;
            if (startNow) {
                response = reservationOrchestrator.reserveInstantInterview(memberId, storyboardId);
            } else {
                response = reservationOrchestrator.reserveInterview(memberId, storyboardId, scheduledAt);
            }

            if (response.isEmpty()) {
                return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
            }

            return ApiResponse.success(response.get(), 201);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }

    @GetMapping("/interview/{reservationId}")
    public ApiResponse getReservationId(@PathVariable UUID reservationId) {
        try {
            log.info("reservationId: {}", reservationId);
            Optional<InterviewReservationResponse> interviewReservation = reservationOrchestrator.getInterviewReservationById(reservationId);

            if (interviewReservation.isEmpty()) {
                return ApiResponse.success(null, 404);
            }

            return ApiResponse.success(interviewReservation.get(), 200);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }


    @GetMapping("/interview/forward")
    public ApiResponse getForwardInterviews(@RequestParam(value = "from", required = false) OffsetDateTime from) {
        try {
            UUID memberId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
            OffsetDateTime fromTime = (from != null) ? from : OffsetDateTime.now();
            Optional<List<InterviewReservationResponse>> interviewsOrEmpty = reservationOrchestrator.getForwardInterviews(memberId, fromTime);

            if (interviewsOrEmpty.isEmpty()) {
                return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
            }

            return ApiResponse.success(interviewsOrEmpty.get(), 200);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }

    @PatchMapping("/interview/{interviewId}/done")
    public ApiResponse doneInterview(@PathVariable UUID interviewId) {
        try {
            boolean result = reservationOrchestrator.markInterviewAsDone(interviewId);

            if (!result) {
                return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
            }

            return ApiResponse.success(null, 200);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }

    @PostMapping("/recap/video")
    public ApiResponse reserveRecap(@RequestBody RecapReservationRequest request) {
        try {
            UUID memberId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
            UUID videoId = UUID.fromString(request.getVideoId());
            ZonedDateTime scheduledAt = request.getScheduledAt();

            Optional<RecapReservationResponse> response = reservationOrchestrator.reserveRecap(memberId, videoId, scheduledAt);

            if (response.isEmpty()) {
                return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
            }

            return ApiResponse.success(response.get(), 201);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }

    @GetMapping("/recap/{recapReservationId}/result")
    public ApiResponse getRecapResult(@PathVariable UUID recapReservationId) {
        Optional<RecapResultResponse> response = reservationOrchestrator.getRecapResult(recapReservationId);

        if (response.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        return ApiResponse.success(response.get(), 200);
    }

    @GetMapping("/recap/{recapReservationId}/audio")
    public ApiResponse getRecapAudio(@PathVariable UUID recapReservationId) {
        Optional<RecapAudioResponse> response = reservationOrchestrator.getRecapAudio(recapReservationId);

        if (response.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        return ApiResponse.success(response.get(), 200);
    }
}
