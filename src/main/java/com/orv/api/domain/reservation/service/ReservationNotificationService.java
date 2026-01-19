package com.orv.api.domain.reservation.service;

import com.orv.api.domain.auth.service.dto.Member;
import com.orv.api.domain.reservation.service.dto.InterviewReservation;
import com.orv.api.domain.reservation.service.dto.jobs.InterviewReservationConfirmedAlimtalkJob;
import com.orv.api.domain.reservation.service.dto.jobs.InterviewReservationPreviewAlimtalkJob;
import com.orv.api.domain.reservation.service.dto.jobs.InterviewReservationTimeReachedAlimtalkJob;
import com.orv.api.domain.storyboard.service.dto.Topic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationNotificationService {
    private final Scheduler scheduler;

    public void notifyInterviewReservationTimeReached(String phoneNumber, OffsetDateTime notifyAt) throws Exception {
        UUID jobId = UUID.randomUUID();
        String jobName = "interviewReservationTimeReachedAlimtalkJob_" + jobId.toString();
        String jobGroup = "notification";

        JobDetail jobDetail = JobBuilder.newJob(InterviewReservationTimeReachedAlimtalkJob.class)
                .withIdentity(jobName, jobGroup)
                .usingJobData("phoneNumber", phoneNumber)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName, jobGroup)
                .startAt(Date.from(notifyAt.toInstant()))
                .withSchedule((SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionFireNow()))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    public void notifyInterviewReservationConfirmed(String phoneNumber, OffsetDateTime notifyAt) throws Exception {
        UUID jobId = UUID.randomUUID();
        String jobName = "interviewReservationConfirmedAlimtalkJob_" + jobId.toString();
        String jobGroup = "notification";

        JobDetail jobDetail = JobBuilder.newJob(InterviewReservationConfirmedAlimtalkJob.class)
                .withIdentity(jobName, jobGroup)
                .usingJobData("phoneNumber", phoneNumber)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName, jobGroup)
                .startAt(Date.from(notifyAt.toInstant()))
                .withSchedule((SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionFireNow()))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    public void notifyInterviewReservationPreview(String phoneNumber, String name, OffsetDateTime scheduledAt, String title, Integer questionCount, UUID reservationId, OffsetDateTime notifyAt) throws Exception {
        UUID jobId = UUID.randomUUID();
        String jobName = "interviewReservationPreviewAlimtalkJob_" + jobId.toString();
        String jobGroup = "notification";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "yyyy. M. d.'('E')' a h:mm",
                Locale.KOREAN
        );

        JobDetail jobDetail = JobBuilder.newJob(InterviewReservationPreviewAlimtalkJob.class)
                .withIdentity(jobName, jobGroup)
                .usingJobData("phoneNumber", phoneNumber)
                .usingJobData("name", name)
                .usingJobData("date", scheduledAt.format(formatter))
                .usingJobData("title", title)
                .usingJobData("questionCount", questionCount)
                .usingJobData("reservationId", reservationId.toString())
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName, jobGroup)
                .startAt(Date.from(notifyAt.toInstant()))
                .withSchedule((SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionFireNow()))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }
}
