package com.orv.api.domain.reservation.service;

import com.orv.api.domain.auth.repository.MemberRepository;
import com.orv.api.domain.auth.service.dto.Member;
import com.orv.api.domain.reservation.service.dto.jobs.InterviewReservationConfirmedAlimtalkJob;
import com.orv.api.domain.reservation.service.dto.jobs.InterviewReservationPreviewAlimtalkJob;
import com.orv.api.domain.reservation.service.dto.jobs.InterviewReservationTimeReachedAlimtalkJob;
import com.orv.api.domain.storyboard.repository.StoryboardRepository;
import com.orv.api.domain.storyboard.service.dto.Scene;
import com.orv.api.domain.storyboard.service.dto.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationNotificationService {
    private final Scheduler scheduler;
    private final MemberRepository memberRepository;
    private final StoryboardRepository storyboardRepository;

    public void sendInterviewConfirmation(UUID memberId, UUID storyboardId, UUID reservationId, OffsetDateTime scheduledAt) {
        try {
            Optional<Member> member = memberRepository.findById(memberId);
            Optional<List<Topic>> topic = storyboardRepository.findTopicsOfStoryboard(storyboardId);
            Optional<List<Scene>> scenes = storyboardRepository.findScenesByStoryboardId(storyboardId);

            if (member.isPresent() && topic.isPresent() && !topic.get().isEmpty() && scenes.isPresent()) {
                String phoneNumber = member.get().getPhoneNumber();
                if (phoneNumber == null) {
                    return;
                }

                Integer questionCount = calculateQuestionCount(scenes.get());
                OffsetDateTime before3Days = scheduledAt.minusDays(3);
                OffsetDateTime notifyPreviewAt = getMaxOffsetDateTime(before3Days, OffsetDateTime.now().plusSeconds(5));

                scheduleInterviewReservationConfirmed(phoneNumber, OffsetDateTime.now().plusSeconds(1));
                scheduleInterviewReservationPreview(
                        phoneNumber,
                        member.get().getNickname(),
                        scheduledAt.withOffsetSameInstant(ZoneOffset.ofHours(9)),
                        topic.get().get(0).getName(),
                        questionCount,
                        reservationId,
                        notifyPreviewAt
                );
                scheduleInterviewReservationTimeReached(phoneNumber, scheduledAt);
            }
        } catch (Exception e) {
            log.error("Failed to send interview confirmation notification for reservation ID: {}", reservationId, e);
        }
    }

    public void sendInstantInterviewPreview(UUID memberId, UUID storyboardId, UUID reservationId) {
        try {
            Optional<Member> member = memberRepository.findById(memberId);
            Optional<List<Topic>> topic = storyboardRepository.findTopicsOfStoryboard(storyboardId);
            Optional<List<Scene>> scenes = storyboardRepository.findScenesByStoryboardId(storyboardId);

            if (member.isPresent() && topic.isPresent() && !topic.get().isEmpty() && scenes.isPresent()) {
                String phoneNumber = member.get().getPhoneNumber();
                if (phoneNumber == null) {
                    return;
                }

                Integer questionCount = calculateQuestionCount(scenes.get());
                
                // Instant interview: Notify immediately
                scheduleInterviewReservationPreview(
                        phoneNumber,
                        member.get().getNickname(),
                        OffsetDateTime.now().plusHours(9), 
                        topic.get().get(0).getName(),
                        questionCount,
                        reservationId,
                        OffsetDateTime.now()
                );
            }
        } catch (Exception e) {
            log.error("Failed to send instant interview preview notification for reservation ID: {}", reservationId, e);
        }
    }

    private void scheduleInterviewReservationTimeReached(String phoneNumber, OffsetDateTime notifyAt) throws SchedulerException {
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

    private void scheduleInterviewReservationConfirmed(String phoneNumber, OffsetDateTime notifyAt) throws SchedulerException {
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

    private void scheduleInterviewReservationPreview(String phoneNumber, String name, OffsetDateTime scheduledAt, String title, Integer questionCount, UUID reservationId, OffsetDateTime notifyAt) throws SchedulerException {
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

    private OffsetDateTime getMaxOffsetDateTime(OffsetDateTime offsetDateTime1, OffsetDateTime offsetDateTime2) {
        return offsetDateTime1.isAfter(offsetDateTime2) ? offsetDateTime1 : offsetDateTime2;
    }

    private Integer calculateQuestionCount(List<Scene> scenes) {
        return Integer.valueOf(
                (int) scenes.stream()
                        .filter(scene -> scene.getSceneType().equals("QUESTION"))
                        .count()
        );
    }
}