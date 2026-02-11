package com.orv.media.repository.jdbc;

import com.orv.media.domain.AudioExtractionJob;
import com.orv.media.domain.AudioExtractionJobStatus;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class AudioExtractionJobRowMapper implements RowMapper<AudioExtractionJob> {
    @Override
    public AudioExtractionJob mapRow(ResultSet rs, int rowNum) throws SQLException {
        AudioExtractionJob job = new AudioExtractionJob();
        job.setId(rs.getLong("id"));
        job.setVideoId(rs.getObject("video_id", UUID.class));
        job.setRecapReservationId(rs.getObject("recap_reservation_id", UUID.class));
        job.setMemberId(rs.getObject("member_id", UUID.class));
        job.setStoryboardId(rs.getObject("storyboard_id", UUID.class));
        job.setResultAudioRecordingId(rs.getObject("result_audio_recording_id", UUID.class));
        job.setStatus(AudioExtractionJobStatus.valueOf(rs.getString("status")));
        job.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        job.setStartedAt(rs.getObject("started_at", LocalDateTime.class));
        return job;
    }
}
