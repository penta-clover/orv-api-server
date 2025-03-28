package com.orv.api.domain.reservation;

import com.orv.api.domain.reservation.dto.InterviewReservation;
import com.orv.api.domain.reservation.dto.InterviewReservationRequest;
import com.orv.api.domain.reservation.dto.RecapReservationRequest;
import com.orv.api.domain.reservation.dto.RecapReservationResponse;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/reservation")
@RequiredArgsConstructor
public class ReservationController {
    private final NotificationSchedulerService notificationService;
    private final ReservationRepository reservationRepository;
    private final RecapRepository recapRepository;

    @PostMapping("/interview")
    public ApiResponse reserveInterview(@RequestBody InterviewReservationRequest request) {
        try {
            UUID memberId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
            UUID storyboardId = UUID.fromString(request.getStoryboardId());
            ZonedDateTime reservedAt = request.getReservedAt();

            Optional<UUID> id = reservationRepository.reserveInterview(memberId, storyboardId,  reservedAt.toLocalDateTime());

            if (id.isEmpty()) {
                return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
            }

            notificationService.scheduleInterviewNotificationCall(memberId, storyboardId, reservedAt);

            return ApiResponse.success(new InterviewReservation(id.get(), memberId, storyboardId, reservedAt.toLocalDateTime(), LocalDateTime.now()), 201);
        } catch (SchedulerException e) {
            e.printStackTrace();
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }


    @GetMapping("/interview/forward")
    public ApiResponse getForwardInterviews(@RequestParam(value = "from", required = false) ZonedDateTime from) {
        try {
            UUID memberId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
            LocalDateTime fromTime = (from != null) ? from.toLocalDateTime() : LocalDateTime.now();
            Optional<List<InterviewReservation>> interviewsOrEmpty = reservationRepository.getReservedInterviews(memberId, fromTime);

            if (interviewsOrEmpty.isEmpty()) {
                return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
            }

            return ApiResponse.success(interviewsOrEmpty.get(), 200);
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

            Optional<UUID> id = recapRepository.reserveRecap(memberId, videoId, scheduledAt.toLocalDateTime());

            if (id.isEmpty()) {
                return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
            }

            return ApiResponse.success(new RecapReservationResponse(id.get(), memberId, videoId, scheduledAt.toLocalDateTime(), LocalDateTime.now()), 201);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }
}
