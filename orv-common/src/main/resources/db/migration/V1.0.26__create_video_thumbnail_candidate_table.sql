CREATE TABLE video_thumbnail_candidate (
    id           BIGSERIAL    PRIMARY KEY,
    job_id       BIGINT       NOT NULL REFERENCES video_thumbnail_extraction_job(id) ON DELETE CASCADE,
    video_id     UUID         NOT NULL REFERENCES video(id) ON DELETE CASCADE,
    timestamp_ms BIGINT       NOT NULL,
    file_key     VARCHAR(512) NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vtc_job_id ON video_thumbnail_candidate(job_id);
CREATE INDEX idx_vtc_video_id ON video_thumbnail_candidate(video_id);
