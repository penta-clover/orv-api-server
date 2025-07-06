package com.orv.api.domain.reservation;

import com.orv.api.domain.reservation.dto.RecapContent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcRecapResultRepositoryImpl implements RecapResultRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RecapRepository recapRepository;

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
}
