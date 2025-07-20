# 🚀 ORV API Server 부하테스트 시스템 구축 계획서

## 📋 목차
1. [개요](#1-개요)
2. [부하테스트 시나리오](#2-부하테스트-시나리오)
3. [부하 분석 및 목표 설정](#3-부하-분석-및-목표-설정)
4. [프로젝트 구조](#4-프로젝트-구조)
5. [구현 계획](#5-구현-계획)
6. [인프라 구성](#6-인프라-구성)
7. [실행 계획](#7-실행-계획)
8. [모니터링 및 분석](#8-모니터링-및-분석)

---

## 1. 개요

본 문서는 ORV API Server의 성능 검증을 위한 부하테스트 시스템 구축 계획을 담고 있습니다. nGrinder를 활용하여 실제 사용자 패턴을 시뮬레이션하고, 시스템의 성능 한계와 병목 지점을 파악하는 것을 목표로 합니다.

### 1.1 테스트 도구
- **부하테스트 도구**: nGrinder
- **스크립트 언어**: Groovy
- **모니터링**: AWS CloudWatch, nGrinder Dashboard
- **분석 도구**: nGrinder Report, CloudWatch Insights

---

## 2. 부하테스트 시나리오

### 2.1 유저 시나리오 A (컨텐츠 생성형)
- **세션 지속 시간**: 10분
- **유저 비율**: 50%
- **시나리오 플로우**:
  1. 로그인 (JWT 토큰 획득)
  2. 대시보드 조회
     - Storyboard 목록 조회
     - Archive 목록 조회
  3. 스토리보드 미리보기 조회 (4회 반복)
  4. 스토리보드 Scene 상세 조회
  5. 7분 분량의 녹화 영상 업로드
  6. 리캡 예약 생성
  7. 녹화 영상 다운로드

### 2.2 유저 시나리오 B (컨텐츠 소비형)
- **세션 지속 시간**: 5분
- **유저 비율**: 50%
- **시나리오 플로우**:
  1. 리캡 목록 조회
  2. 오디오 스트리밍 (실시간 재생)
  3. 리캡 상세 내용 조회

---

## 3. 부하 분석 및 목표 설정

### 3.1 사용자 부하 분석

#### 기본 메트릭
- **MAU (Monthly Active Users)**: 6,000명 (서비스 분기 목표)
- **사용 패턴**: 각 유저는 일주일 중 4일에 걸쳐 시나리오 A, B를 각 2회씩 수행
- **피크 시간대**: 매일 19:00~24:00 (UTC+9)

#### 계산된 메트릭
```
DAU_avg = MAU × (4/7) ≈ 3,429명
HAU_avg = DAU / 5 ≈ 686명
SessionDuration_avg = 10분 × 0.5 + 5분 × 0.5 = 7.5분
CAU_avg = (HAU_avg × SessionDuration_avg) / 60분 ≈ 86명
```

### 3.2 성능 목표
- **Throughput**: 최소 100 TPS 이상
- **Latency**: 
  - 95 percentile: 500ms 이하
  - 99 percentile: 1000ms 이하
- **CPU Utilization**: 70% 이하 유지
- **에러율**: 0.1% 이하

---

## 4. 프로젝트 구조

### 4.1 디렉토리 구조
```
orv-api-server/
├── src/test/java/com/orv/api/load/
│   ├── scenarios/
│   │   ├── UserScenarioA.groovy          # 컨텐츠 생성 시나리오
│   │   ├── UserScenarioB.groovy          # 컨텐츠 소비 시나리오
│   │   └── MasterScenario.groovy         # 전체 부하 패턴 제어
│   ├── config/
│   │   ├── LoadTestConfig.java           # 부하테스트 환경 설정
│   │   ├── TestDataProvider.java         # 테스트 데이터 제공
│   │   └── LoadPattern.java              # 부하 패턴 정의
│   ├── utils/
│   │   ├── ApiEndpoints.java             # API 엔드포인트 상수
│   │   ├── AuthHelper.java               # JWT 인증 헬퍼
│   │   ├── MediaHelper.java              # 미디어 업로드/다운로드 헬퍼
│   │   └── MetricsCollector.java         # 성능 메트릭 수집
│   └── data/
│       ├── DataGenerator.java            # Fake 데이터 생성기
│       └── TestDataSeeder.java           # DB 시드 데이터 생성
│
└── src/test/resources/ngrinder/
    ├── test-videos/
    │   └── sample-7min-video.mp4        # 7분 테스트 영상
    ├── test-data/
    │   ├── users.json                    # 테스트 사용자 데이터
    │   └── storyboards.json              # 테스트 스토리보드 데이터
    └── config/
        └── ngrinder.properties           # nGrinder 설정
```

### 4.2 주요 컴포넌트 설명

#### scenarios/UserScenarioA.groovy
```groovy
// 주요 기능:
// - 로그인 및 JWT 토큰 관리
// - 병렬 API 호출 (대시보드)
// - 대용량 파일 업로드 (7분 영상)
// - 응답 시간 측정 및 검증
```

#### scenarios/UserScenarioB.groovy
```groovy
// 주요 기능:
// - 리캡 데이터 조회
// - 오디오 스트리밍 시뮬레이션
// - 청크 단위 데이터 처리
```

#### config/LoadPattern.java
```java
// 부하 패턴 정의:
// - Ramp-up: 10분간 0 → 86 VU
// - Peak 1: 10분간 86 VU 유지
// - Spike: 5분간 86 → 258 VU
// - Peak 2: 10분간 86 VU 유지
```

---

## 5. 구현 계획

### 5.1 Phase 1: 기초 환경 구축 (2일)
- [ ] nGrinder 디렉토리 구조 생성
- [ ] Gradle 의존성 추가
- [ ] 기본 헬퍼 클래스 구현
- [ ] API 엔드포인트 매핑

### 5.2 Phase 2: 테스트 데이터 준비 (3일)
- [ ] 테스트 전용 OAuth Provider 구현
  - [ ] TestAuthService 클래스 구현
  - [ ] SocialAuthServiceFactory 수정
- [ ] 6,000명 테스트 사용자 DB 생성 스크립트
- [ ] 스토리보드/토픽 데이터 생성
- [ ] 7분 테스트 영상 준비 (다양한 해상도)
- [ ] S3 업로드 스크립트 작성

### 5.3 Phase 3: 시나리오 구현 (4일)
- [ ] UserScenarioA 구현
  - [ ] 로그인 로직
  - [ ] 대시보드 조회 (병렬 처리)
  - [ ] 영상 업로드 (multipart)
  - [ ] 리캡 예약
- [ ] UserScenarioB 구현
  - [ ] 리캡 조회
  - [ ] 오디오 스트리밍
- [ ] MasterScenario 구현
  - [ ] VU 분배 로직
  - [ ] 부하 패턴 제어

### 5.4 Phase 4: 테스트 실행 및 분석 (2일)
- [ ] 파일럿 테스트 (10 VU)
- [ ] 본 테스트 실행
- [ ] 결과 분석 및 리포트 작성

---

## 6. 인프라 구성

### 6.1 기존 환경 활용
**AWS 인프라는 이미 구축되어 있으므로 별도 구성 작업이 불필요합니다.**

#### 확인이 필요한 기존 리소스
- **API Server**: EC2 인스턴스 (운영 환경과 동일한 스펙)
- **Database**: RDS PostgreSQL 15
- **Storage**: S3 버킷
- **nGrinder**: Controller 및 Agent 서버

### 6.2 사전 환경 체크리스트
- [ ] API 서버 접근 가능 여부 확인
- [ ] 데이터베이스 연결 상태 확인
- [ ] S3 버킷 읽기/쓰기 권한 확인
- [ ] nGrinder Controller/Agent 상태 확인
- [ ] 네트워크 대역폭 및 보안그룹 설정 확인

---

## 7. 실행 계획

### 7.1 부하 테스트 시나리오
```
총 실행 시간: 35분

1. Ramp-up (10분)
   - 0 → 43 VU (Scenario A)
   - 0 → 43 VU (Scenario B)
   - 점진적 증가

2. Peak 1 (10분)
   - 43 VU 유지 (Scenario A)
   - 43 VU 유지 (Scenario B)
   - 총 86 VU 안정화

3. Spike (5분)
   - 43 → 129 VU (Scenario A)
   - 43 → 129 VU (Scenario B)
   - 총 258 VU (3배 부하)

4. Peak 2 (10분)
   - 129 → 43 VU (Scenario A)
   - 129 → 43 VU (Scenario B)
   - 총 86 VU로 복귀
```

### 7.2 실행 체크리스트
- [ ] 테스트 데이터 검증
- [ ] API 서버 헬스체크
- [ ] 모니터링 대시보드 준비
- [ ] 네트워크 대역폭 확인
- [ ] 로그 수집 설정

---

## 8. 모니터링 및 분석

### 8.1 실시간 모니터링
- **nGrinder Dashboard**
  - TPS, Mean Test Time
  - Error Rate
  - Active Users
  
- **CloudWatch Dashboard**
  - EC2: CPU, Memory, Network
  - RDS: Connections, IOPS, CPU
  - S3: Request Rate, Bandwidth

### 8.2 로그 수집
- Application Logs: CloudWatch Logs
- Access Logs: S3
- Error Logs: CloudWatch Logs Insights

### 8.3 분석 메트릭
```
성능 지표:
- Throughput (TPS)
- Response Time Distribution
- Error Rate by API
- Resource Utilization

병목 분석:
- Slow Query Analysis
- API Response Time Breakdown
- Network Latency
- I/O Wait Time
```

### 8.4 리포트 템플릿
1. Executive Summary
2. Test Configuration
3. Performance Metrics
4. Bottleneck Analysis
5. Recommendations
6. Appendix (Raw Data)

---

## 📝 참고사항

### 테스트 데이터 생성 가이드

#### 1. 테스트 전용 OAuth Provider 구현
```java
// TestAuthService.java - 테스트 환경에서만 활성화
@ConditionalOnProperty(name = "test.auth.enabled", havingValue = "true")
@Component
public class TestAuthService implements SocialAuthService {
    
    @Override
    public String getAuthorizationUrl(String state) {
        return "http://localhost:8080/test-oauth-callback";
    }
    
    @Override
    public SocialUserInfo getUserInfo(String code) {
        // authorization code를 파싱해서 테스트 유저 정보 반환
        String userId = code.replace("test_user_", "");
        
        return SocialUserInfo.builder()
            .provider("test")
            .id("fake_social_id_" + userId)
            .email("loadtest_" + userId + "@test.com")
            .name("LoadTest" + userId)
            .build();
    }
}
```

#### 2. 테스트 사용자 DB 생성
```sql
-- 6,000명의 테스트 사용자 생성
INSERT INTO member (id, nickname, provider, social_id, email, profile_image_url, created_at)
SELECT 
  uuid_generate_v4(),
  'LoadTest' || generate_series,
  'test',  -- 테스트 provider 사용
  'fake_social_id_' || generate_series,
  'loadtest_' || generate_series || '@test.com',
  'https://test-bucket.s3.amazonaws.com/profile/default.png',
  CURRENT_TIMESTAMP
FROM generate_series(1, 6000);
```

#### 3. nGrinder에서의 인증 처리
```groovy
// UserScenarioA.groovy
@Test
public void testScenario() {
    // 1. 로그인 시뮬레이션
    def userId = getRandomUserId()  // 1~6000
    def authCode = "test_user_${userId}"
    
    // OAuth 콜백 시뮬레이션
    def response = request.GET(
        "${baseUrl}/api/v0/auth/callback/test?code=${authCode}"
    )
    
    // JWT 토큰 추출
    def token = extractTokenFromRedirectUrl(response)
    
    // 2. 이후 API 호출에 토큰 사용
    def headers = ["Authorization": "Bearer ${token}"]
    
    // 대시보드 조회, 스토리보드 조회 등...
}
```

### 주의사항
1. 테스트 환경은 프로덕션과 완전히 분리
2. 테스트 후 리소스 정리 필수
3. 비용 모니터링 설정
4. 보안 그룹 설정 확인

### 연락처
- 부하테스트 담당: [담당자명]
- 인프라 지원: [인프라팀]
- 긴급 연락처: [연락처]

---

*이 문서는 ORV API Server 부하테스트 프로젝트의 공식 가이드입니다.*
*최종 수정일: 2025년 7월 20일*
