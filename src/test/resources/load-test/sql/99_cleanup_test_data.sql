-- ORV API Server Load Test Data Cleanup Script
-- Phase 0: 부하테스트 데이터 정리 스크립트
-- 외래키 제약사항을 고려한 순서로 삭제

-- 1. 정리 전 현재 상태 확인
SELECT 
    'Before Cleanup' as status,
    (SELECT COUNT(*) FROM recap_answer_summary ras 
     JOIN recap_result rr ON ras.recap_result_id = rr.id 
     JOIN recap_reservation rv ON rv.recap_result_id = rr.id 
     JOIN member m ON rv.member_id = m.id 
     WHERE m.provider = 'test') as answer_summaries,
    (SELECT COUNT(*) FROM recap_reservation rr 
     JOIN member m ON rr.member_id = m.id 
     WHERE m.provider = 'test') as recap_reservations,
    (SELECT COUNT(*) FROM recap_result rr 
     JOIN recap_reservation rv ON rv.recap_result_id = rr.id 
     JOIN member m ON rv.member_id = m.id 
     WHERE m.provider = 'test') as recap_results,
    (SELECT COUNT(*) FROM interview_audio_recording iar 
     JOIN member m ON iar.member_id = m.id 
     WHERE m.provider = 'test') as audio_recordings,
    (SELECT COUNT(*) FROM video v 
     JOIN member m ON v.member_id = m.id 
     WHERE m.provider = 'test') as videos,
    (SELECT COUNT(*) FROM member_role mr 
     JOIN member m ON mr.member_id = m.id 
     WHERE m.provider = 'test') as member_roles,
    (SELECT COUNT(*) FROM term_agreement ta 
     JOIN member m ON ta.member_id = m.id 
     WHERE m.provider = 'test') as term_agreements,
    (SELECT COUNT(*) FROM member WHERE provider = 'test') as test_members;

-- 2. 외래키 제약사항 순서에 따른 삭제

-- 2-1. recap_answer_summary 삭제 (가장 하위 테이블)
DELETE FROM recap_answer_summary 
WHERE recap_result_id IN (
    SELECT rr.id 
    FROM recap_result rr
    JOIN recap_reservation rv ON rv.recap_result_id = rr.id
    JOIN member m ON rv.member_id = m.id
    WHERE m.provider = 'test'
);

-- 2-2. recap_reservation 삭제 (recap_result, interview_audio_recording, video, member 참조)
DELETE FROM recap_reservation 
WHERE member_id IN (
    SELECT id FROM member WHERE provider = 'test'
);

-- 2-3. recap_result 삭제 (더 이상 참조되지 않음)
DELETE FROM recap_result 
WHERE id NOT IN (
    SELECT DISTINCT recap_result_id 
    FROM recap_reservation 
    WHERE recap_result_id IS NOT NULL
);

-- 2-4. interview_audio_recording 삭제 (member, storyboard 참조, 더 이상 recap_reservation에서 참조되지 않음)
DELETE FROM interview_audio_recording 
WHERE member_id IN (
    SELECT id FROM member WHERE provider = 'test'
);

-- 2-5. video 삭제 (member, storyboard 참조, 더 이상 recap_reservation에서 참조되지 않음)
DELETE FROM video 
WHERE member_id IN (
    SELECT id FROM member WHERE provider = 'test'
);

-- 2-6. storyboard_usage_history 삭제 (있다면)
DELETE FROM storyboard_usage_history 
WHERE member_id IN (
    SELECT id FROM member WHERE provider = 'test'
);

-- 2-7. member_role 삭제 (member 참조)
DELETE FROM member_role 
WHERE member_id IN (
    SELECT id FROM member WHERE provider = 'test'
);

-- 2-8. term_agreement 삭제 (member 참조)
DELETE FROM term_agreement 
WHERE member_id IN (
    SELECT id FROM member WHERE provider = 'test'
);

-- 2-9. member 삭제 (최상위 테이블)
DELETE FROM member WHERE provider = 'test';

-- 3. 정리 후 상태 확인
SELECT 
    'After Cleanup' as status,
    (SELECT COUNT(*) FROM recap_answer_summary ras 
     JOIN recap_result rr ON ras.recap_result_id = rr.id 
     JOIN recap_reservation rv ON rv.recap_result_id = rr.id 
     JOIN member m ON rv.member_id = m.id 
     WHERE m.provider = 'test') as answer_summaries,
    (SELECT COUNT(*) FROM recap_reservation rr 
     JOIN member m ON rr.member_id = m.id 
     WHERE m.provider = 'test') as recap_reservations,
    (SELECT COUNT(*) FROM recap_result rr 
     JOIN recap_reservation rv ON rv.recap_result_id = rr.id 
     JOIN member m ON rv.member_id = m.id 
     WHERE m.provider = 'test') as recap_results,
    (SELECT COUNT(*) FROM interview_audio_recording iar 
     JOIN member m ON iar.member_id = m.id 
     WHERE m.provider = 'test') as audio_recordings,
    (SELECT COUNT(*) FROM video v 
     JOIN member m ON v.member_id = m.id 
     WHERE m.provider = 'test') as videos,
    (SELECT COUNT(*) FROM member_role mr 
     JOIN member m ON mr.member_id = m.id 
     WHERE m.provider = 'test') as member_roles,
    (SELECT COUNT(*) FROM term_agreement ta 
     JOIN member m ON ta.member_id = m.id 
     WHERE m.provider = 'test') as term_agreements,
    (SELECT COUNT(*) FROM member WHERE provider = 'test') as test_members;

-- 4. 고아 레코드 정리 (혹시 남아있을 수 있는 recap_result)
DELETE FROM recap_result 
WHERE id NOT IN (
    SELECT DISTINCT recap_result_id 
    FROM recap_reservation 
    WHERE recap_result_id IS NOT NULL
);

-- 5. 최종 확인
SELECT 'Cleanup Completed Successfully' as status;
