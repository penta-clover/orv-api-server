CREATE TABLE video_thumbnail_extraction_job (
    id BIGSERIAL PRIMARY KEY,
    video_id UUID NOT NULL REFERENCES video(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP
);

CREATE INDEX idx_video_thumbnail_extraction_job_claimable
ON video_thumbnail_extraction_job(id, started_at)
WHERE status = 'PENDING' OR status = 'PROCESSING';

ALTER TABLE video_thumbnail_extraction_job SET (
    autovacuum_vacuum_scale_factor = 0,
    autovacuum_vacuum_threshold = 1000,
    autovacuum_vacuum_cost_delay = 0,
    fillfactor = 70
);