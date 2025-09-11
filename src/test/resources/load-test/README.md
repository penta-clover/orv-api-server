# ORV API Server 부하테스트 준비 완료

## Phase 0: 필수 보완사항 해결 ✅ 완료

부하테스트 계획서(`docs/load-test/`)에 따라 Phase 0의 필수 보완사항들을 모두 구현했습니다.

### 🔧 구현 완료 사항

#### A. TestAuthService 보안 강화 구현 ✅
- **TestAuthService 클래스** (`src/main/java/com/orv/api/domain/auth/TestAuthService.java`)
  - SocialAuthService 인터페이스 구현
  - test_user_ 접두사 강제 검증
  - 사용자 ID 범위 검증 (1-6000)
  - @ConditionalOnProperty 및 @Profile 적용
- **SocialAuthServiceFactory 수정** 
  - test provider 지원 추가
  - Optional 타입으로 조건부 주입

#### B. DB Connection Pool 최적화 ✅
- **application-loadtest.properties** (`src/main/resources/application-loadtest.properties`)
  - HikariCP 설정: maximum-pool-size=50, minimum-idle=20
  - JPA/Hibernate 배치 처리 설정: batch_size=25
  - Tomcat 최적화: threads.max=100, max-connections=200
  - 테스트 인증 활성화: test.auth.enabled=true

#### C. 테스트 데이터 스크립트 준비 ✅
- **6,000명 사용자 생성**: `sql/01_create_test_users.sql`
  - member, member_role 테이블 데이터 생성
  - UUID 및 제약사항 검증
- **기존 리캡 데이터 생성**: `sql/02_create_recap_data.sql`
  - video, interview_audio_recording 생성
  - recap_result, recap_reservation, recap_answer_summary 생성
  - 시나리오 B 테스트용 완성된 리캡 데이터
- **데이터 정리 스크립트**: `sql/99_cleanup_test_data.sql`
  - 외래키 순서 고려한 안전한 삭제

#### D. 자동화 스크립트 ✅
- **setup_load_test.sh**: 전체 과정 자동화
  - DB 연결 확인
  - 기존 데이터 확인 및 정리
  - 테스트 데이터 생성
  - 애플리케이션 빌드 및 테스트

## 🚀 사용 방법

### 1. 자동 설정 (권장)
```bash
# 프로젝트 루트에서 실행
cd src/test/resources/load-test
./setup_load_test.sh
```

### 2. 수동 설정
```bash
# 1. 테스트 사용자 생성
psql -d orv_api -f sql/01_create_test_users.sql

# 2. 리캡 데이터 생성
psql -d orv_api -f sql/02_create_recap_data.sql

# 3. 애플리케이션 빌드
./gradlew build

# 4. loadtest 프로파일로 실행
./gradlew bootRun --args='--spring.profiles.active=loadtest'
```

### 3. 테스트 인증 확인
```bash
# 애플리케이션 실행 후
curl 'http://localhost:8080/api/v0/auth/callback/test?code=test_user_1'
```

## 📊 생성되는 테스트 데이터

### 테스트 사용자 (6,000명)
- **Provider**: test
- **Nickname**: LT000001 ~ LT006000
- **Social ID**: fake_social_id_1 ~ fake_social_id_6000
- **Email**: loadtest_1@test.com ~ loadtest_6000@test.com
- **권한**: USER 역할 자동 부여

### 시나리오 B용 리캡 데이터
- **비디오**: 사용자당 7개 (총 42,000개)
- **오디오 녹음**: 비디오당 1개 (7-9분 분량)
- **리캡 결과**: 오디오당 1개
- **답변 요약**: 리캡당 평균 8개 Scene

## 🔍 검증 및 문제 해결

### 데이터 생성 확인
```sql
-- 테스트 사용자 수 확인
SELECT COUNT(*) FROM member WHERE provider = 'test';  -- 6000

-- 리캡 데이터 확인
SELECT 
    COUNT(DISTINCT v.id) as videos,
    COUNT(DISTINCT iar.id) as audio_recordings,
    COUNT(DISTINCT rr.id) as recap_reservations
FROM member m
LEFT JOIN video v ON m.id = v.member_id
LEFT JOIN interview_audio_recording iar ON m.id = iar.member_id  
LEFT JOIN recap_reservation rr ON m.id = rr.member_id
WHERE m.provider = 'test';
```

### 애플리케이션 설정 확인
```bash
# TestAuthService 빈 등록 확인
curl http://localhost:8080/actuator/beans | grep -i testauth

# 설정값 확인
curl http://localhost:8080/actuator/configprops | grep -i test.auth
```

### 데이터 정리
```bash
# 테스트 데이터만 정리
./setup_load_test.sh --cleanup-only
```

## 📋 다음 단계: Phase 1

Phase 0 완료 후 다음 단계로 진행하세요:

1. **Phase 1: 기초 환경 구축** (2일)
   - nGrinder 프로젝트 설정
   - Gradle 의존성 추가
   - 헬퍼 클래스 구현 (AuthHelper, MediaHelper, MetricsCollector)

2. **Phase 2: 테스트 데이터 준비** (1일)
   - 기존 8개 스토리보드 검증
   - TestDataProvider 클래스 구현
   - 테스트 영상 및 S3 설정

3. **Phase 3: nGrinder 스크립트 개발** (3일)
   - UserScenarioA/B 구현
   - MasterScenario 구현

4. **Phase 4: 테스트 실행 및 분석** (2일)
   - 파일럿 테스트
   - 본 테스트 실행 및 결과 분석

## 🎯 핵심 아키텍처

### 테스트 인증 플로우
```
nGrinder Script → TestAuthService → JWT Token → API 호출
     ↓              ↓
  test_user_1    fake_social_id_1
```

### 시나리오별 데이터 사용
- **시나리오 A (50%)**: 새로운 비디오 업로드 → 리캡 생성
- **시나리오 B (50%)**: 기존 리캡 결과 조회 → 오디오 스트리밍

## 📞 지원

문제 발생 시:
1. `setup_load_test.sh` 로그 확인
2. 애플리케이션 로그에서 TestAuthService 관련 오류 확인
3. DB 연결 및 테스트 데이터 존재 여부 확인

---

**✅ Phase 0 완료 - 부하테스트 기반 환경 준비 완료!**
