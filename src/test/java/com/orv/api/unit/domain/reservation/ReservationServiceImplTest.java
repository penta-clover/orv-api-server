package com.orv.api.unit.domain.reservation;

import static org.assertj.core.api.Assertions.catchException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.orv.api.domain.auth.MemberRepository;
import com.orv.api.domain.auth.dto.Member;
import com.orv.api.domain.reservation.RecapRepository;
import com.orv.api.domain.reservation.ReservationNotificationService;
import com.orv.api.domain.reservation.ReservationRepository;
import com.orv.api.domain.reservation.ReservationServiceImpl;
import com.orv.api.domain.reservation.dto.InterviewReservation;
import com.orv.api.domain.storyboard.StoryboardRepository;
import com.orv.api.domain.storyboard.dto.Topic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;

import javax.sound.midi.SysexMessage;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private StoryboardRepository storyboardRepository;

    @Mock
    private RecapRepository recapRepository;

    @Mock
    private ReservationNotificationService notificationService;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    // reserveInterview 메소드 테스트 (정상 케이스: 예약 성공 및 알림 발송)
    @Test
    public void testReserveInterviewSuccessWithNotification() throws Exception {
        // given
        UUID memberId = UUID.randomUUID();
        UUID storyboardId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        OffsetDateTime reservedAt = OffsetDateTime.now();
        UUID interviewId = UUID.randomUUID();

        when(reservationRepository.reserveInterview(eq(memberId), eq(storyboardId), any()))
                .thenReturn(Optional.of(interviewId));

        when(reservationRepository.findInterviewReservationById(interviewId))
                .thenReturn(Optional.of(new InterviewReservation(
                        interviewId,
                        memberId,
                        storyboardId,
                        reservedAt.toLocalDateTime(),
                        ZonedDateTime.now().toLocalDateTime()
                )));

        when(storyboardRepository.findTopicsOfStoryboard(any()))
                .thenReturn(Optional.of(Arrays.asList(new Topic(
                                topicId,
                                "name",
                                "description",
                                "thumbnail_url",
                                Collections.emptyList()
                        )
                )));

        when(storyboardRepository.findScenesByStoryboardId(storyboardId))
                .thenReturn(Optional.of(new ArrayList<>()));

        Member member = new Member();
        member.setPhoneNumber("01012345678");
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when
        Optional<UUID> result = reservationService.reserveInterview(memberId, storyboardId, reservedAt);

        // then
        verify(notificationService, times(1))
                .notifyInterviewReservationConfirmed(eq("01012345678"), any(OffsetDateTime.class));

        assertTrue(result.isPresent());
        assertEquals(interviewId, result.get());
    }

    // reserveInterview 메소드 테스트 (예약 실패: 예약 id가 비어있는 경우)
    @Test
    public void testReserveInterviewFailureNoId() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID storyboardId = UUID.randomUUID();
        OffsetDateTime reservedAt = OffsetDateTime.now();

        when(reservationRepository.reserveInterview(eq(memberId), eq(storyboardId), any()))
                .thenReturn(Optional.empty());

        // when
        Exception exception = catchException(() -> {
            reservationService.reserveInterview(memberId, storyboardId, reservedAt);
        });

        // then
        assertNotNull(exception);
        assertEquals("Failed to reserve interview", exception.getMessage());
    }

    // reserveInterview 메소드 테스트 (알림 스케줄러 예외 발생)
    @Test
    public void testReserveInterviewNotificationSchedulerException() throws Exception {
        // given
        UUID memberId = UUID.randomUUID();
        UUID storyboardId = UUID.randomUUID();
        OffsetDateTime reservedAt = OffsetDateTime.now();
        UUID interviewId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();

        when(reservationRepository.reserveInterview(eq(memberId), eq(storyboardId), any()))
                .thenReturn(Optional.of(interviewId));

        when(reservationRepository.findInterviewReservationById(interviewId))
                .thenReturn(Optional.of(new InterviewReservation(
                        interviewId,
                        memberId,
                        storyboardId,
                        reservedAt.toLocalDateTime(),
                        ZonedDateTime.now().toLocalDateTime()
                )));

        when(storyboardRepository.findTopicsOfStoryboard(any()))
                .thenReturn(Optional.of(Arrays.asList(new Topic(
                                topicId,
                                "name",
                                "description",
                                "thumbnail_url",
                                Collections.emptyList()
                        )
                )));

        when(storyboardRepository.findScenesByStoryboardId(storyboardId))
                .thenReturn(Optional.of(new ArrayList<>()));

        Member member = new Member();
        member.setPhoneNumber("01012345678");
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        doThrow(new SchedulerException("Scheduler error"))
                .when(notificationService).notifyInterviewReservationConfirmed(eq("01012345678"), any(OffsetDateTime.class));

        // when
        Exception exception = catchException(() -> {
            reservationService.reserveInterview(memberId, storyboardId, reservedAt);
        });

        // then
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Failed to schedule interview notification"));
    }

    // getForwardInterviews 메소드 테스트
    @Test
    public void testGetForwardInterviews() {
        // given
        UUID memberId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now();
        List<InterviewReservation> reservations = Arrays.asList(new InterviewReservation(), new InterviewReservation());

        when(reservationRepository.getReservedInterviews(eq(memberId), eq(from)))
                .thenReturn(Optional.of(reservations));

        // when
        Optional<List<InterviewReservation>> result = reservationService.getForwardInterviews(memberId, from);

        // then
        assertTrue(result.isPresent());
        assertEquals(2, result.get().size());
    }

    // markInterviewAsDone 메소드 테스트
    @Test
    public void testMarkInterviewAsDone() {
        // given
        UUID interviewId = UUID.randomUUID();
        when(reservationRepository.changeInterviewReservationStatus(eq(interviewId), eq("done")))
                .thenReturn(true);

        // when
        boolean result = reservationService.markInterviewAsDone(interviewId);

        // then
        assertTrue(result);
    }

    // reserveRecap 메소드 테스트
    @Test
    public void testReserveRecap() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        ZonedDateTime scheduledAt = ZonedDateTime.now();
        UUID recapId = UUID.randomUUID();

        when(recapRepository.reserveRecap(eq(memberId), eq(videoId), any()))
                .thenReturn(Optional.of(recapId));

        // when
        Optional<UUID> result = reservationService.reserveRecap(memberId, videoId, scheduledAt);

        // then
        assertTrue(result.isPresent());
        assertEquals(recapId, result.get());
    }
}
