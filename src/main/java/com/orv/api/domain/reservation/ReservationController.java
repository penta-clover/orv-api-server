package com.orv.api.domain.reservation;

import com.orv.api.domain.reservation.dto.InterviewReservationRequest;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/reservation")
@RequiredArgsConstructor
public class ReservationController {
    private final NotificationSchedulerService notificationService;

    @PostMapping("/interview")
    public ApiResponse reserveInterview(@RequestBody InterviewReservationRequest request) {
        try {
            UUID memberId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
            UUID storyboardId = UUID.fromString(request.getStoryboardId());
            ZonedDateTime reservedAt = request.getReservedAt();

            notificationService.scheduleInterviewNotificationCall(memberId, storyboardId, reservedAt);
            return ApiResponse.success(null, 201);
        } catch (SchedulerException e) {
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }
}
