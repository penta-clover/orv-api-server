package com.orv.api.domain.reservation;

import com.orv.api.domain.auth.MemberRepository;
import com.orv.api.domain.auth.dto.Member;
import com.orv.api.domain.reservation.dto.InterviewReservation;
import com.orv.api.domain.storyboard.StoryboardRepository;
import com.orv.api.domain.storyboard.dto.Scene;
import com.orv.api.domain.storyboard.dto.Topic;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {
    private final ReservationRepository reservationRepository;
    private final ReservationNotificationService notificationService;
    private final MemberRepository memberRepository;
    private final StoryboardRepository storyboardRepository;

    @Override
    public Optional<InterviewReservation> getInterviewReservationById(UUID reservationId) {
        return reservationRepository.findInterviewReservationById(reservationId);
    }

    @Override
    public Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, OffsetDateTime reservedAt) throws Exception {
        try {
            Optional<UUID> id = reservationRepository.reserveInterview(memberId, storyboardId, reservedAt.toLocalDateTime());

            if (id.isEmpty()) {
                throw new Exception("Failed to reserve interview");
            }

            // 인터뷰 관련 정보 가져오기
            Optional<Member> member = memberRepository.findById(memberId);
            Optional<InterviewReservation> reservation = reservationRepository.findInterviewReservationById(id.get());
            Optional<List<Topic>> topic = storyboardRepository.findTopicsOfStoryboard(reservation.get().getStoryboardId());

            // preview 링크는 인터뷰로부터 3일 전에 발송. 단, 예약일이 인터뷰 3일 이내인 경우 즉시 발송.
            OffsetDateTime before3Days = reservedAt.minusDays(3);
            OffsetDateTime notifyPreviewAt = getMaxOffsetDateTime(before3Days, OffsetDateTime.now().plusSeconds(5));

            // 인터뷰에 포함된 질문 개수 계산
            Optional<List<Scene>> scenesOrEmpty = storyboardRepository.findScenesByStoryboardId(storyboardId);
            Integer questionCount = calculateQuestionCount(scenesOrEmpty.get());

            // 전화번호 정보가 있다면 알림톡 발송
            if (member.get().getPhoneNumber() != null) {
                notificationService.notifyInterviewReservationConfirmed(member.get().getPhoneNumber(), OffsetDateTime.now().plusSeconds(1));
                notificationService.notifyInterviewReservationPreview(member.get().getPhoneNumber(), member.get().getNickname(), reservation.get().getScheduledAt().atOffset(ZoneOffset.ofHours(9)), topic.get().get(0).getName(), questionCount, reservation.get().getId(), notifyPreviewAt);
                notificationService.notifyInterviewReservationTimeReached(member.get().getPhoneNumber(), reservedAt);
            }

            return id;
        } catch (SchedulerException e) {
            throw new Exception("Failed to schedule interview notification", e);
        }
    }

    @Override
    public Optional<UUID> reserveInstantInterview(UUID memberId, UUID storyboardId) throws Exception {
        try {
            OffsetDateTime scheduledAt = OffsetDateTime.now();
            Optional<UUID> id = reservationRepository.reserveInterview(memberId, storyboardId, scheduledAt.toLocalDateTime().plusHours(9));
            reservationRepository.changeInterviewReservationStatus(id.get(), "done");

            if (id.isEmpty()) {
                throw new Exception("Failed to reserve instant interview");
            }

            // 인터뷰 관련 정보 가져오기
            Optional<Member> member = memberRepository.findById(memberId);
            Optional<InterviewReservation> reservation = reservationRepository.findInterviewReservationById(id.get());
            Optional<List<Topic>> topic = storyboardRepository.findTopicsOfStoryboard(reservation.get().getStoryboardId());

            // 인터뷰에 포함된 질문 개수 계산
            Optional<List<Scene>> scenesOrEmpty = storyboardRepository.findScenesByStoryboardId(storyboardId);
            Integer questionCount = calculateQuestionCount(scenesOrEmpty.get());

            // 전화번호 정보가 있다면 알림톡 발송
            if (member.get().getPhoneNumber() != null) {
                notificationService.notifyInterviewReservationPreview(member.get().getPhoneNumber(), member.get().getNickname(), reservation.get().getScheduledAt().atOffset(ZoneOffset.ofHours(9)), topic.get().get(0).getName(), questionCount, reservation.get().getId(), OffsetDateTime.now());
            }

            return id;
        } catch (SchedulerException e) {
            throw new Exception("Failed to schedule interview notification", e);
        }
    }

    @Override
    public Optional<List<InterviewReservation>> getForwardInterviews(UUID memberId, OffsetDateTime from) {
        return reservationRepository.getReservedInterviews(memberId, from);
    }

    @Override
    public boolean markInterviewAsDone(UUID interviewId) {
        return reservationRepository.changeInterviewReservationStatus(interviewId, "done");
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
