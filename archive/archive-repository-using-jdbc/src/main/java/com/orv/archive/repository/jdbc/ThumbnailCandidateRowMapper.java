package com.orv.archive.repository.jdbc;

import com.orv.archive.domain.ThumbnailCandidate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

public class ThumbnailCandidateRowMapper implements RowMapper<ThumbnailCandidate> {
    @Override
    public ThumbnailCandidate mapRow(ResultSet rs, int rowNum) throws SQLException {
        ThumbnailCandidate candidate = new ThumbnailCandidate();
        candidate.setId(rs.getLong("id"));
        candidate.setJobId(rs.getLong("job_id"));
        candidate.setVideoId(UUID.fromString(rs.getString("video_id")));
        candidate.setTimestampMs(rs.getLong("timestamp_ms"));
        candidate.setFileKey(rs.getString("file_key"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            candidate.setCreatedAt(createdAt.toLocalDateTime());
        }

        return candidate;
    }
}