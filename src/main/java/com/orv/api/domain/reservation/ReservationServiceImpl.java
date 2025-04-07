package com.orv.api.domain.reservation;

import com.orv.api.domain.auth.MemberRepository;
import com.orv.api.domain.auth.dto.Member;
import com.orv.api.domain.reservation.dto.InterviewReservation;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {
    private final ReservationRepository reservationRepository;
    private final RecapRepository recapRepository;
    private final ReservationNotificationService notificationService;
    private final MemberRepository memberRepository;

    @Override
    public Optional<InterviewReservation> getInterviewReservationById(UUID reservationId) {
        return reservationRepository.findInterviewReservationById(reservationId);
    }

    @Override
    public Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, ZonedDateTime reservedAt) throws Exception {
        try {
            Optional<UUID> id = reservationRepository.reserveInterview(memberId, storyboardId, reservedAt.toLocalDateTime());

            if (id.isEmpty()) {
                throw new Exception("Failed to reserve interview");
            }

            Optional<Member> member = memberRepository.findById(memberId);

            if (member.get().getPhoneNumber() != null) {
                notificationService.notifyInterviewReservationConfirmed(member.get().getPhoneNumber(), OffsetDateTime.now().plusSeconds(1));
                notificationService.notifyInterviewReservationTimeReached(member.get().getPhoneNumber(), reservedAt.toOffsetDateTime());
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

    @Override
    public Optional<UUID> reserveRecap(UUID memberId, UUID videoId, ZonedDateTime scheduledAt) {
        return recapRepository.reserveRecap(memberId, videoId, scheduledAt.toLocalDateTime());
    }
}
