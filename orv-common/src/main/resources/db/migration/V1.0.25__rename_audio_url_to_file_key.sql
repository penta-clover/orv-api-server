-- audio_url 컬럼을 audio_file_key로 변경하고 기존 CDN URL에서 file key만 추출
ALTER TABLE interview_audio_recording RENAME COLUMN audio_url TO audio_file_key;

UPDATE interview_audio_recording
SET audio_file_key = regexp_replace(audio_file_key, '^https?://[^/]+/', '');
