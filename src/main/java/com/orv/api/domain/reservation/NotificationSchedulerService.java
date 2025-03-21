package com.orv.api.domain.reservation;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class NotificationSchedulerService {
    @Autowired
    private Scheduler scheduler;

    public void scheduleInterviewNotificationCall(UUID memberId, UUID storyboardId, ZonedDateTime executionTime) throws SchedulerException {
        UUID jobId = UUID.randomUUID();
        String jobName = "interviewAlimtalkJob_" + jobId.toString();
        String jobGroup = "notification";

        JobDetail jobDetail = JobBuilder.newJob(InterviewAlimtalkJob.class)
                .withIdentity(jobName, jobGroup)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName, jobGroup)
                .startAt(Date.from(executionTime.toInstant()))
                .withSchedule((SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionFireNow()))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }
}
