package com.orv.api.domain.recap.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.orv.api.domain.recap.controller.dto.RecapAnswerSummaryResponse;
import com.orv.api.domain.recap.service.dto.RecapContent;
import com.orv.api.domain.recap.controller.dto.RecapResultResponse;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcRecapResultRepository implements RecapResultRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RecapReservationRepository recapRepository;

    @Override
    @Transactional
    public Optional<UUID> save(UUID recapReservationId, List<RecapContent> contents) {
        // 1. Create recap_result and get its ID
        SimpleJdbcInsert jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("recap_result")
                .usingGeneratedKeyColumns("id");
        Map<String, Object> keys = jdbcInsert.executeAndReturnKeyHolder(Map.of()).getKeys();
        if (keys == null || keys.get("id") == null) {
            return Optional.empty();
        }
        UUID recapResultId = (UUID) keys.get("id");

        // 2. Link recap_result to recap_reservation
        recapRepository.linkRecapResult(recapReservationId, recapResultId);

        // 3. Batch insert all answer summaries
        String sql = "INSERT INTO recap_answer_summary (recap_result_id, scene_id, summary, scene_order) VALUES (?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                RecapContent content = contents.get(i);
                ps.setObject(1, recapResultId);
                ps.setObject(2, content.getSceneId());
                ps.setString(3, content.getAnswerSummary());
                ps.setInt(4, i);
            }

            @Override
            public int getBatchSize() {
                return contents.size();
            }
        });

        return Optional.of(recapResultId);
    }

    @Override
    public Optional<RecapResultResponse> findByRecapReservationId(UUID recapReservationId) {
        // Query 1: Get recap_result_id and created_at
        String queryRecapResult = """
                SELECT
                    reservation.recap_result_id,
                    result.created_at
                FROM
                    recap_reservation AS reservation
                JOIN
                    recap_result AS result ON reservation.recap_result_id = result.id
                WHERE
                    reservation.id = ?
                """;

        List<Map<String, Object>> recapResultRows = jdbcTemplate.queryForList(queryRecapResult, recapReservationId);

        if (recapResultRows.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> recapResultRow = recapResultRows.get(0);
        UUID recapResultId = (UUID) recapResultRow.get("recap_result_id");
        OffsetDateTime createdAt = ((java.sql.Timestamp) recapResultRow.get("created_at")).toInstant().atOffset(ZoneOffset.UTC);

        // Query 2: Get recap answer summaries and scene questions
        String queryAnswerSummaries = """
                SELECT
                    summary.scene_id,
                    scene.content->>'question' AS question,
                    summary.summary
                FROM
                    recap_answer_summary AS summary
                JOIN
                    scene ON scene.id = summary.scene_id
                WHERE
                    summary.recap_result_id = ?
                ORDER BY
                    summary.scene_order
                """;

        RowMapper<RecapAnswerSummaryResponse> answerSummaryRowMapper = (rs, rowNum) ->
                new RecapAnswerSummaryResponse(
                        (UUID) rs.getObject("scene_id"),
                        rs.getString("question"), // Directly get 'question' from DB
                        rs.getString("summary")
                );

        List<RecapAnswerSummaryResponse> answerSummaries = jdbcTemplate.query(queryAnswerSummaries, answerSummaryRowMapper, recapResultId);

        return Optional.of(new RecapResultResponse(recapResultId, createdAt, answerSummaries));
    }
}
