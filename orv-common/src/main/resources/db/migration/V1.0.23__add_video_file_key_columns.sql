-- 새 컬럼 추가
ALTER TABLE video ADD COLUMN video_file_key TEXT;
ALTER TABLE video ADD COLUMN thumbnail_file_key TEXT;

-- 기존 CDN URL에서 도메인 prefix를 제거하여 file key 추출
-- video_url: 'https://xxx.cloudfront.net/archive/videos/uuid' → 'archive/videos/uuid'
-- thumbnail_url: 'https://xxx.cloudfront.net/archive/images/uuid' → 'archive/images/uuid'
UPDATE video
SET video_file_key = regexp_replace(video_url, '^https?://[^/]+/', '')
WHERE video_url IS NOT NULL;

UPDATE video
SET thumbnail_file_key = regexp_replace(thumbnail_url, '^https?://[^/]+/', '')
WHERE thumbnail_url IS NOT NULL;
