package com.orv.recap.repository.jdbc;

import com.orv.recap.domain.RecapAnswerSummaryInfo;
import com.orv.recap.domain.RecapResultInfo;
import com.orv.recap.repository.RecapReservationRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecapResultRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RecapReservationRepository recapReservationRepository;

    @InjectMocks
    private JdbcRecapResultRepository recapResultRepository;

    @Test
    @DisplayName("리캡 예약 ID로 리캡 결과와 답변 요약을 조회한다")
    void findByRecapReservationId_success() {
        // Given
        UUID recapReservationId = UUID.randomUUID();
        UUID recapResultId = UUID.randomUUID();
        UUID sceneId1 = UUID.fromString("b33dbf34-7f5d-47db-84f6-0c846eeb0b6a");
        UUID sceneId2 = UUID.fromString("b7ca99d7-55d6-4eb1-9102-8957c1275ee5");

        // Mock the first query (queryForList) - returns recap_result_id and created_at
        Map<String, Object> recapResultRow = new HashMap<>();
        recapResultRow.put("recap_result_id", recapResultId);
        recapResultRow.put("created_at", Timestamp.from(Instant.now()));
        List<Map<String, Object>> recapResultRows = List.of(recapResultRow);

        when(jdbcTemplate.queryForList(anyString(), eq(recapReservationId)))
                .thenReturn(recapResultRows);

        // Mock the second query (query with RowMapper) - returns answer summaries
        List<RecapAnswerSummaryInfo> answerSummaries = List.of(
                new RecapAnswerSummaryInfo(sceneId1, "가벼운 인사 한마디 부탁 드립니다.", "안녕하세요. 저는 홍길동입니다."),
                new RecapAnswerSummaryInfo(sceneId2, "@{name}님은 왜 HySpark에 들어 오려고 했나요?", "HySpark에 대한 기대가 컸습니다.")
        );

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(recapResultId)))
                .thenReturn(answerSummaries);

        // When
        Optional<RecapResultInfo> result = recapResultRepository.findByRecapReservationId(recapReservationId);

        // Then
        assertThat(result).isPresent();
        RecapResultInfo response = result.get();
        assertThat(response.getRecapResultId()).isEqualTo(recapResultId);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getAnswerSummaries()).hasSize(2);

        RecapAnswerSummaryInfo summary1 = response.getAnswerSummaries().get(0);
        assertThat(summary1.getSceneId()).isEqualTo(sceneId1);
        assertThat(summary1.getQuestion()).isEqualTo("가벼운 인사 한마디 부탁 드립니다.");
        assertThat(summary1.getAnswerSummary()).isEqualTo("안녕하세요. 저는 홍길동입니다.");

        RecapAnswerSummaryInfo summary2 = response.getAnswerSummaries().get(1);
        assertThat(summary2.getSceneId()).isEqualTo(sceneId2);
        assertThat(summary2.getQuestion()).isEqualTo("@{name}님은 왜 HySpark에 들어 오려고 했나요?");
        assertThat(summary2.getAnswerSummary()).isEqualTo("HySpark에 대한 기대가 컸습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 리캡 예약 ID로 조회 시 Optional.empty()를 반환한다")
    void findByRecapReservationId_notFound() {
        // Given
        UUID nonExistentRecapReservationId = UUID.randomUUID();

        when(jdbcTemplate.queryForList(anyString(), eq(nonExistentRecapReservationId)))
                .thenReturn(Collections.emptyList());

        // When
        Optional<RecapResultInfo> result = recapResultRepository
                .findByRecapReservationId(nonExistentRecapReservationId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("리캡 예약은 존재하지만 연결된 리캡 결과가 없는 경우 Optional.empty()를 반환한다")
    void findByRecapReservationId_noRecapResultLinked() {
        // Given
        UUID recapReservationId = UUID.randomUUID();

        // The JOIN query returns empty when recap_result_id is NULL (no join match)
        when(jdbcTemplate.queryForList(anyString(), eq(recapReservationId)))
                .thenReturn(Collections.emptyList());

        // When
        Optional<RecapResultInfo> result = recapResultRepository
                .findByRecapReservationId(recapReservationId);

        // Then
        assertThat(result).isEmpty();
    }
}
