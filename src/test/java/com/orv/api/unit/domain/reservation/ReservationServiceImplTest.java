package com.orv.api.unit.domain.reservation;

import static org.assertj.core.api.Assertions.catchException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.orv.api.domain.reservation.repository.ReservationRepository;
import com.orv.api.domain.reservation.service.ReservationServiceImpl;
import com.orv.api.domain.reservation.service.dto.InterviewReservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @Test
    public void testReserveInterviewSuccess() throws Exception {
        // given
        UUID memberId = UUID.randomUUID();
        UUID storyboardId = UUID.randomUUID();
        OffsetDateTime reservedAt = OffsetDateTime.now();
        UUID interviewId = UUID.randomUUID();

        when(reservationRepository.reserveInterview(eq(memberId), eq(storyboardId), any()))
                .thenReturn(Optional.of(interviewId));

        // when
        Optional<UUID> result = reservationService.reserveInterview(memberId, storyboardId, reservedAt);

        // then
        assertTrue(result.isPresent());
        assertEquals(interviewId, result.get());
    }

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
}