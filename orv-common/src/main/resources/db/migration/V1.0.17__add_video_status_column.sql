-- status 컬럼 추가 (기존 데이터는 UPLOADED로)
ALTER TABLE video ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED';

-- nullable 변경 (v1 API에서는 PENDING 상태에서 이 값들이 없음)
ALTER TABLE video ALTER COLUMN running_time DROP NOT NULL;
ALTER TABLE video ALTER COLUMN video_url DROP NOT NULL;
ALTER TABLE video ALTER COLUMN thumbnail_url DROP NOT NULL;

-- status 조회 성능을 위한 인덱스
CREATE INDEX idx_video_status ON video(status);
