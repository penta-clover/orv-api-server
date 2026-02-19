package com.orv.reservation.controller;

import com.orv.reservation.orchestrator.InterviewReservationOrchestrator;
import com.orv.reservation.controller.dto.*;
import com.orv.reservation.orchestrator.dto.*;
import com.orv.common.dto.ApiResponse;
import com.orv.common.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/reservation/interview")
@RequiredArgsConstructor
@Slf4j
public class InterviewReservationController {
    private final InterviewReservationOrchestrator reservationOrchestrator;

    @PostMapping
    public ApiResponse reserveInterview(@RequestBody InterviewReservationRequest request, @RequestParam(value="startNow", required = false, defaultValue = "false") Boolean startNow) throws Exception {
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
    }

    @GetMapping("/{reservationId}")
    public ApiResponse getReservationId(@PathVariable UUID reservationId) {
        log.info("reservationId: {}", reservationId);
        Optional<InterviewReservationResponse> interviewReservation = reservationOrchestrator.getInterviewReservationById(reservationId);

        if (interviewReservation.isEmpty()) {
            return ApiResponse.success(null, 404);
        }

        return ApiResponse.success(interviewReservation.get(), 200);
    }


    @GetMapping("/forward")
    public ApiResponse getForwardInterviews(@RequestParam(value = "from", required = false) OffsetDateTime from) {
        UUID memberId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        OffsetDateTime fromTime = (from != null) ? from : OffsetDateTime.now();
        Optional<List<InterviewReservationResponse>> interviewsOrEmpty = reservationOrchestrator.getForwardInterviews(memberId, fromTime);

        if (interviewsOrEmpty.isEmpty()) {
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }

        return ApiResponse.success(interviewsOrEmpty.get(), 200);
    }

    @GetMapping("/list")
    public ApiResponse getReservations(
            @RequestParam(value = "from", required = false) OffsetDateTime from,
            @RequestParam(value = "to", required = false) OffsetDateTime to,
            @RequestParam(value = "sort", required = false, defaultValue = "desc") String sort,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size,
            @RequestParam(value = "isUsed", required = false) Boolean isUsed
    ) {
        UUID memberId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        return ApiResponse.success(reservationOrchestrator.getReservations(memberId, from, to, sort, safePage, safeSize, isUsed), 200);
    }

    @PatchMapping("/{interviewId}/done")
    public ApiResponse doneInterview(@PathVariable UUID interviewId) {
        boolean result = reservationOrchestrator.markInterviewAsDone(interviewId);

        if (!result) {
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }

        return ApiResponse.success(null, 200);
    }

    @PostMapping("/{reservationId}/start")
    public ApiResponse startReservation(@PathVariable UUID reservationId) {
        UUID memberId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        reservationOrchestrator.startReservation(reservationId, memberId);
        return ApiResponse.success(null, 200);
    }
}
