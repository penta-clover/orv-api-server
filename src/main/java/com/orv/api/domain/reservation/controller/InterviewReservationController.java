package com.orv.api.domain.reservation.controller;

import com.orv.api.domain.reservation.orchestrator.InterviewReservationOrchestrator;
import com.orv.api.domain.reservation.controller.dto.*;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
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

    @GetMapping("/{reservationId}")
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


    @GetMapping("/forward")
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

    @PatchMapping("/{interviewId}/done")
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
}
