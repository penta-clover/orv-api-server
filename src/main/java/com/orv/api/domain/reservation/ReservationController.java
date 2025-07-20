package com.orv.api.domain.reservation;

import com.orv.api.domain.reservation.dto.InterviewReservation;
import com.orv.api.domain.reservation.dto.InterviewReservationRequest;
import com.orv.api.domain.reservation.dto.RecapAudioResponse;
import com.orv.api.domain.reservation.dto.RecapReservationRequest;
import com.orv.api.domain.reservation.dto.RecapReservationResponse;
import com.orv.api.domain.reservation.dto.RecapResultResponse;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import jakarta.websocket.server.PathParam;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    private final ReservationService reservationService;
    private final RecapService recapService;

    @PostMapping("/interview")
    public ApiResponse reserveInterview(@RequestBody InterviewReservationRequest request, @RequestParam(value="startNow", required = false, defaultValue = "false") Boolean startNow) {
        try {
            UUID memberId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
            UUID storyboardId = UUID.fromString(request.getStoryboardId());
            Optional<UUID> reservationId;
            OffsetDateTime scheduledAt = request.getReservedAt() != null ? request.getReservedAt().toOffsetDateTime() : OffsetDateTime.now();

            if (startNow) {
                reservationId = reservationService.reserveInstantInterview(memberId, storyboardId);
            } else {
                reservationId = reservationService.reserveInterview(memberId, storyboardId, scheduledAt);
            }

            if (reservationId.isEmpty()) {
                return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
            }

            return ApiResponse.success(new InterviewReservation(reservationId.get(), memberId, storyboardId, scheduledAt.toLocalDateTime(), LocalDateTime.now()), 201);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }

    @GetMapping("/interview/{reservationId}")
    public ApiResponse getReservationId(@PathVariable UUID reservationId) {
        try {
            log.info("reservationId: {}", reservationId);
            Optional<InterviewReservation> interviewReservation = reservationService.getInterviewReservationById(reservationId);

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
            Optional<List<InterviewReservation>> interviewsOrEmpty = reservationService.getForwardInterviews(memberId, fromTime);

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
            boolean result = reservationService.markInterviewAsDone(interviewId);

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

            Optional<UUID> id = recapService.reserveRecap(memberId, videoId, scheduledAt);

            if (id.isEmpty()) {
                return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
            }

            return ApiResponse.success(new RecapReservationResponse(id.get(), memberId, videoId, scheduledAt.toLocalDateTime(), LocalDateTime.now()), 201);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }

    @GetMapping("/recap/{recapReservationId}/result")
    public ApiResponse getRecapResult(@PathVariable UUID recapReservationId) {
        Optional<RecapResultResponse> recapResult = recapService.getRecapResult(recapReservationId);

        if (recapResult.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        return ApiResponse.success(recapResult.get(), 200);
    }

    @GetMapping("/recap/{recapReservationId}/audio")
    public ApiResponse getRecapAudio(@PathVariable UUID recapReservationId) {
        Optional<RecapAudioResponse> recapAudio = recapService.getRecapAudio(recapReservationId);

        if (recapAudio.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        return ApiResponse.success(recapAudio.get(), 200);
    }
}
