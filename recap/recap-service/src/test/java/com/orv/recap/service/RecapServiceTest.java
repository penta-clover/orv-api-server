package com.orv.recap.service;

import com.orv.recap.repository.RecapReservationRepository;
import com.orv.recap.repository.RecapResultRepository;
import com.orv.recap.domain.RecapAnswerSummaryInfo;
import com.orv.recap.domain.RecapResultInfo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecapServiceTest {

    @InjectMocks
    private RecapServiceImpl recapService;

    @Mock
    private RecapReservationRepository recapReservationRepository;

    @Mock
    private RecapResultRepository recapResultRepository;

    @Test
    @DisplayName("리캡 예약 ID로 리캡 결과를 성공적으로 조회한다")
    void getRecapResult_success() {
        // Given
        UUID recapReservationId = UUID.randomUUID();
        UUID recapResultId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();
        List<RecapAnswerSummaryInfo> answerSummaries = List.of(
                new RecapAnswerSummaryInfo(UUID.randomUUID(), "Question 1", "Summary 1"),
                new RecapAnswerSummaryInfo(UUID.randomUUID(), "Question 2", "Summary 2")
        );
        RecapResultInfo expectedResponse = new RecapResultInfo(recapResultId, createdAt, answerSummaries);

        when(recapResultRepository.findByRecapReservationId(recapReservationId))
                .thenReturn(Optional.of(expectedResponse));

        // When
        Optional<RecapResultInfo> result = recapService.getRecapResult(recapReservationId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getRecapResultId()).isEqualTo(expectedResponse.getRecapResultId());
        assertThat(result.get().getCreatedAt()).isEqualTo(expectedResponse.getCreatedAt());
        assertThat(result.get().getAnswerSummaries()).hasSize(2);
        assertThat(result.get().getAnswerSummaries().get(0).getSceneId()).isEqualTo(answerSummaries.get(0).getSceneId());
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
        Optional<RecapResultInfo> result = recapService.getRecapResult(nonExistentRecapReservationId);

        // Then
        assertThat(result).isEmpty();
    }
}
