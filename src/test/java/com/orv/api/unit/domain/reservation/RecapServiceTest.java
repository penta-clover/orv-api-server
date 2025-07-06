package com.orv.api.unit.domain.reservation;

import com.orv.api.domain.reservation.RecapResultRepository;
import com.orv.api.domain.reservation.RecapService;
import com.orv.api.domain.reservation.dto.RecapAnswerSummaryResponse;
import com.orv.api.domain.reservation.dto.RecapResultResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
class RecapServiceTest {

    @Autowired
    private RecapService recapService;

    @MockitoBean
    private RecapResultRepository recapResultRepository;

    @Test
    @DisplayName("리캡 예약 ID로 리캡 결과를 성공적으로 조회한다")
    void getRecapResult_success() {
        // Given
        UUID recapReservationId = UUID.randomUUID();
        UUID recapResultId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();
        List<RecapAnswerSummaryResponse> answerSummaries = List.of(
                new RecapAnswerSummaryResponse(UUID.randomUUID(), "Question 1", "Summary 1"),
                new RecapAnswerSummaryResponse(UUID.randomUUID(), "Question 2", "Summary 2")
        );
        RecapResultResponse expectedResponse = new RecapResultResponse(recapResultId, createdAt, answerSummaries);

        when(recapResultRepository.findByRecapReservationId(recapReservationId))
                .thenReturn(Optional.of(expectedResponse));

        // When
        Optional<RecapResultResponse> result = recapService.getRecapResult(recapReservationId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("존재하지 않는 리캡 예약 ID로 조회 시 빈 Optional을 반환한다")
    void getRecapResult_notFound() {
        // Given
        UUID nonExistentRecapReservationId = UUID.randomUUID();

        // Mocking the behavior of the mocked RecapResultRepository
        when(recapResultRepository.findByRecapReservationId(nonExistentRecapReservationId))
                .thenReturn(Optional.empty());

        // When
        Optional<RecapResultResponse> result = recapService.getRecapResult(nonExistentRecapReservationId);

        // Then
        assertThat(result).isEmpty();
    }
}
