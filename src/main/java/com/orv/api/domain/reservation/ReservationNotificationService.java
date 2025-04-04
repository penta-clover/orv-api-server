package com.orv.api.domain.reservation;

import com.orv.api.domain.reservation.jobs.InterviewReservationConfirmedAlimtalkJob;
import com.orv.api.domain.reservation.jobs.InterviewReservationTimeReachedAlimtalkJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.OffsetDateTime;
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
}
