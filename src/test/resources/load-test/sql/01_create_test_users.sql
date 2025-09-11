-- ORV API Server Load Test Data Creation Script
-- Phase 0: 6,000명의 테스트 사용자 생성 스크립트 (agent_process_thread 형식)
-- 실행 전 확인: PostgreSQL에서 gen_random_uuid() 함수 사용 가능한지 확인
-- 
-- nGrinder 분산 환경 지원:
-- - agent: 0-9 (10개 에이전트)
-- - process: 0-9 (에이전트당 10개 프로세스)
-- - thread: 0-59 (프로세스당 60개 쓰레드)
-- - 총 6,000명 = 10 * 10 * 60

-- 1. 기본 역할 생성 (없다면)
INSERT INTO role (id, name) 
VALUES (gen_random_uuid(), 'USER')
ON CONFLICT (name) DO NOTHING;

-- 2. 6,000명의 테스트 사용자 생성 (agent_process_thread 형식)
INSERT INTO member (
    id, 
    nickname, 
    provider, 
    social_id, 
    email, 
    profile_image_url,
    name,
    gender,
    phone_number,
    created_at
)
SELECT 
    gen_random_uuid(),
    'LT_' || agent_num || '-' || process_num || '-' || thread_num,  -- LT_0-0-1 형식
    'test',
    'fake_social_id_' || agent_num || '_' || process_num || '_' || thread_num,  -- fake_social_id_0_0_1 형식
    'loadtest_' || agent_num || '-' || process_num || '-' || thread_num || '@test.com',  -- loadtest_0-0-1@test.com 형식
    'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-profile.png',
    'LoadTest_' || agent_num || '_' || process_num || '_' || thread_num,
    CASE WHEN (agent_num + process_num + thread_num) % 2 = 0 THEN 'MALE' ELSE 'FEMALE' END,
    '010-' || LPAD(((agent_num * 100 + process_num * 10 + thread_num) % 10000)::text, 4, '0') || '-' || 
    LPAD(((agent_num * 100 + process_num * 10 + thread_num) % 10000)::text, 4, '0'),
    CURRENT_TIMESTAMP - INTERVAL '1 day' * (random() * 365)  -- 지난 1년 내 랜덤 가입일
FROM (
    SELECT 
        a.num as agent_num,
        p.num as process_num,
        t.num as thread_num
    FROM 
        generate_series(0, 9) as a(num),    -- 10개 에이전트
        generate_series(0, 9) as p(num),    -- 에이전트당 10개 프로세스
        generate_series(0, 59) as t(num)    -- 프로세스당 60개 쓰레드
) as coords;

-- 3. 모든 테스트 사용자에게 USER 권한 부여
INSERT INTO member_role (member_id, role_id)
SELECT 
    m.id,
    r.id
FROM member m, role r
WHERE m.provider = 'test' 
  AND r.name = 'USER';

-- 4. 생성 결과 확인
SELECT 
    'Test Users Created' as status,
    COUNT(*) as total_test_users,
    MIN(created_at) as earliest_created,
    MAX(created_at) as latest_created
FROM member 
WHERE provider = 'test';

-- 5. 권한 부여 결과 확인
SELECT 
    'Roles Assigned' as status,
    COUNT(*) as total_role_assignments
FROM member_role mr
JOIN member m ON mr.member_id = m.id
WHERE m.provider = 'test';
