-- ORV API Server Load Test Data Creation Script
-- Phase 0: 시나리오 B용 기존 리캡 데이터 생성 스크립트

-- 1. 기존 스토리보드 데이터가 있는지 확인 (실제 DB에서 확인 필요)
SELECT 'Existing Storyboards Check' as status, COUNT(*) as total_storyboards FROM storyboard;

-- 2. 각 테스트 사용자당 5-10개의 비디오 생성 (총 약 45,000개)
-- 실제 스토리보드 ID는 DB에서 확인 후 수정 필요
INSERT INTO video (
    id,
    storyboard_id,
    member_id,
    video_url,
    thumbnail_url,
    title,
    created_at
)
SELECT 
    gen_random_uuid(),
    -- 스토리보드 순환 선택 (실제 스토리보드 ID로 교체 필요)
    (ARRAY[
        (SELECT id FROM storyboard LIMIT 1 OFFSET 0),
        (SELECT id FROM storyboard LIMIT 1 OFFSET 1),
        (SELECT id FROM storyboard LIMIT 1 OFFSET 2),
        (SELECT id FROM storyboard LIMIT 1 OFFSET 3),
        (SELECT id FROM storyboard LIMIT 1 OFFSET 4),
        (SELECT id FROM storyboard LIMIT 1 OFFSET 5),
        (SELECT id FROM storyboard LIMIT 1 OFFSET 6),
        (SELECT id FROM storyboard LIMIT 1 OFFSET 7)
    ])[(ROW_NUMBER() OVER (PARTITION BY m.id ORDER BY random()) - 1) % 8 + 1],
    m.id,
    'https://d3bdjeyz3ry3pi.cloudfront.net/videos/loadtest/' || 
    EXTRACT(EPOCH FROM m.created_at)::bigint || '_' || 
    (ROW_NUMBER() OVER (PARTITION BY m.id ORDER BY random())) || '.mp4',
    'https://d3bdjeyz3ry3pi.cloudfront.net/thumbnails/loadtest/' ||
    EXTRACT(EPOCH FROM m.created_at)::bigint || '_' || 
    (ROW_NUMBER() OVER (PARTITION BY m.id ORDER BY random())) || '.jpg',
    'LoadTest Video ' || (ROW_NUMBER() OVER (PARTITION BY m.id ORDER BY random())),
    m.created_at + INTERVAL '1 hour' * (random() * 24 * 30)  -- 지난 한 달 내 랜덤 생성
FROM member m,
     generate_series(1, 7) as video_count  -- 각 사용자당 7개 비디오
WHERE m.provider = 'test'  -- 테스트 사용자만
ORDER BY m.id, video_count;

-- 3. 각 비디오에 대해 interview_audio_recording 생성
INSERT INTO interview_audio_recording (
    id,
    storyboard_id,
    member_id,
    audio_url,
    running_time,
    created_at
)
SELECT 
    gen_random_uuid(),
    v.storyboard_id,
    v.member_id,
    'https://d3bdjeyz3ry3pi.cloudfront.net/audios/loadtest/' ||
    EXTRACT(EPOCH FROM v.created_at)::bigint || '_' || 
    SUBSTRING(v.id::text, 1, 8) || '.mp3',
    420 + (random() * 120)::int,  -- 7-9분 랜덤 (420-540초)
    v.created_at + INTERVAL '10 minutes'
FROM video v
WHERE EXISTS (
    SELECT 1 FROM member m 
    WHERE m.id = v.member_id AND m.provider = 'test'
);

-- 4. recap_result 생성 (각 audio_recording당 1개)
INSERT INTO recap_result (
    id,
    created_at
)
SELECT 
    gen_random_uuid(),
    iar.created_at + INTERVAL '5 minutes'  -- 오디오 생성 5분 후
FROM interview_audio_recording iar
JOIN member m ON iar.member_id = m.id
WHERE m.provider = 'test';

-- 5. recap_reservation 생성 (video와 audio_recording, recap_result 연결)
INSERT INTO recap_reservation (
    id,
    member_id,
    video_id,
    scheduled_at,
    created_at,
    interview_audio_recording_id,
    recap_result_id
)
SELECT 
    gen_random_uuid(),
    v.member_id,
    v.id,
    iar.created_at + INTERVAL '2 minutes',  -- 스케줄링 시간
    iar.created_at,
    iar.id,
    rr.id
FROM video v
JOIN interview_audio_recording iar ON v.storyboard_id = iar.storyboard_id 
    AND v.member_id = iar.member_id
    AND v.created_at <= iar.created_at  -- 같은 사용자의 비디오와 매칭
JOIN recap_result rr ON rr.created_at >= iar.created_at 
    AND rr.created_at <= iar.created_at + INTERVAL '10 minutes'
JOIN member m ON v.member_id = m.id
WHERE m.provider = 'test'
  AND NOT EXISTS (
      SELECT 1 FROM recap_reservation rr2 
      WHERE rr2.video_id = v.id
  )  -- 중복 방지
ORDER BY v.member_id, v.created_at
LIMIT 42000;  -- 약 6,000명 × 7개 = 42,000개

-- 6. recap_answer_summary 생성 (각 recap_result당 6-9개 Scene 답변)
-- Scene 데이터가 필요하므로 실제 스토리보드 구조 확인 후 수정
INSERT INTO recap_answer_summary (
    id,
    recap_result_id,
    scene_id,
    summary,
    scene_order,
    created_at
)
SELECT 
    gen_random_uuid(),
    rr.id,
    -- 실제 Scene ID로 교체 필요 (스토리보드별 Scene 체인 구조)
    (SELECT id FROM scene WHERE storyboard_id = rv.storyboard_id LIMIT 1 OFFSET (scene_num - 1)),
    'LoadTest 답변 요약 ' || scene_num || ': 이것은 부하테스트를 위한 더미 답변 요약입니다. ' ||
    '실제 인터뷰 내용을 반영한 의미있는 답변 내용이 여기에 포함됩니다.',
    scene_num,
    rr.created_at + INTERVAL '1 minute' * scene_num
FROM recap_result rr
JOIN recap_reservation rv ON rv.recap_result_id = rr.id
JOIN member m ON rv.member_id = m.id,
generate_series(1, 8) as scene_num  -- 평균 8개 Scene (실제로는 6-9개)
WHERE m.provider = 'test'
  AND EXISTS (
      SELECT 1 FROM scene s 
      WHERE s.storyboard_id = rv.storyboard_id
      LIMIT 1 OFFSET (scene_num - 1)
  );  -- 해당 Scene이 실제로 존재하는 경우만

-- 7. 생성 결과 확인
SELECT 
    'Recap Data Creation Summary' as status,
    (SELECT COUNT(*) FROM video v JOIN member m ON v.member_id = m.id WHERE m.provider = 'test') as total_videos,
    (SELECT COUNT(*) FROM interview_audio_recording iar JOIN member m ON iar.member_id = m.id WHERE m.provider = 'test') as total_audio_recordings,
    (SELECT COUNT(*) FROM recap_reservation rr JOIN member m ON rr.member_id = m.id WHERE m.provider = 'test') as total_recap_reservations,
    (SELECT COUNT(*) FROM recap_result rr JOIN recap_reservation rv ON rv.recap_result_id = rr.id JOIN member m ON rv.member_id = m.id WHERE m.provider = 'test') as total_recap_results,
    (SELECT COUNT(*) FROM recap_answer_summary ras JOIN recap_result rr ON ras.recap_result_id = rr.id JOIN recap_reservation rv ON rv.recap_result_id = rr.id JOIN member m ON rv.member_id = m.id WHERE m.provider = 'test') as total_answer_summaries;

-- 8. 시나리오 B 검증용 - 테스트 사용자별 리캡 데이터 샘플 조회
SELECT 
    m.nickname,
    COUNT(DISTINCT rr.id) as recap_count,
    COUNT(DISTINCT ras.id) as answer_summary_count,
    MIN(iar.running_time) as min_audio_length,
    MAX(iar.running_time) as max_audio_length
FROM member m
LEFT JOIN recap_reservation rr ON m.id = rr.member_id
LEFT JOIN interview_audio_recording iar ON rr.interview_audio_recording_id = iar.id
LEFT JOIN recap_answer_summary ras ON rr.recap_result_id = ras.recap_result_id
WHERE m.provider = 'test'
  AND m.nickname LIKE 'LT00000%'  -- 처음 10명 샘플
GROUP BY m.id, m.nickname
ORDER BY m.nickname
LIMIT 10;
