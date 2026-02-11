CREATE TABLE audio_extraction_job (
    id                    BIGSERIAL PRIMARY KEY,
    video_id              UUID NOT NULL REFERENCES video(id) ON DELETE CASCADE,
    recap_reservation_id  UUID,
    member_id             UUID NOT NULL REFERENCES member(id),
    storyboard_id         UUID NOT NULL,
    result_audio_recording_id UUID,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at            TIMESTAMP
);

CREATE INDEX idx_audio_extraction_job_claimable
    ON audio_extraction_job(id, started_at)
    WHERE status = 'PENDING' OR status = 'PROCESSING';
