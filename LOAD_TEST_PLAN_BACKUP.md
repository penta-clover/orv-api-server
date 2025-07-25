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
- **세션 지속 시간**: 8-11분 (Scene 개수별 차별화)
  - 6개 Scene: 약 8분
  - 8개 Scene: 약 10분  
  - 9개 Scene: 약 11분
- **유저 비율**: 50%
- **목표**: 인터뷰 영상 녹화 → 업로드 → 리캡 생성 → 영상 다운로드 전체 플로우 테스트

#### 시나리오 상세 플로우

| 단계 | HTTP Method | Endpoint | 설명 | 대기시간 | 예상 응답시간 |
|------|-------------|----------|------|---------|--------------|
| 1 | GET | `/api/v0/auth/callback/test` | 테스트 전용 로그인 | - | < 200ms |
| 2 | GET | `/api/v0/topic/list` | 토픽 목록 조회 | 1초 | < 300ms |
| 3 | GET | `/api/v0/archive/videos/my` | 내 영상 목록 조회 | 0.5초 | < 500ms |
| 4 | GET | `/api/v0/storyboard/{id}/preview` | 스토리보드 미리보기 (4회 반복) | 각 2초 | < 400ms |
| 5 | GET | `/api/v0/storyboard/scene/{sceneId}` | Scene 상세 조회 (6-9회, 스토리보드별 다름) | 각 50초 | < 300ms |
| 6 | POST | `/api/v0/archive/recorded-video` | 7분 영상 업로드 (480p, ~5MB) | - | 3-10초 |
| 7 | POST | `/api/v0/reservation/recap/video` | 리캡 예약 생성 | 2초 | < 500ms |
| 8 | GET | `/api/v0/archive/video/{videoId}` | 영상 다운로드 | - | 5-20초 |

#### 주요 특징
- **총 API 호출 횟수**: 18-25회 (Scene 개수에 따라 가변)
  - 6개 Scene: 18회 (로그인 1 + 조회 13 + 생성 2 + 다운로드 1)
  - 8개 Scene: 21회 (로그인 1 + 조회 16 + 생성 2 + 다운로드 1)  
  - 9개 Scene: 22회 (로그인 1 + 조회 17 + 생성 2 + 다운로드 1)
- **예상 총 소요시간**: 8-11분 (Scene 개수별 차별화)
- **파일 처리**: 영상 업로드(480p, ~5MB) 및 다운로드
- **병목 예상 지점**: 영상 업로드, 영상 다운로드, Scene 장시간 세션 유지

### 2.2 유저 시나리오 B (컨텐츠 소비형)
- **세션 지속 시간**: 5분
- **유저 비율**: 50%
- **목표**: 기존 리캡 결과 조회 및 오디오 청취

#### 시나리오 상세 플로우

| 단계 | HTTP Method | Endpoint | 설명 | 대기시간 | 예상 응답시간 |
|------|-------------|----------|------|---------|--------------|
| 1 | GET | `/api/v0/auth/callback/test` | 테스트 전용 로그인 | - | < 200ms |
| 2 | GET | `/api/v0/reservation/recap/{id}/result` | 리캡 결과 조회 | 2초 | < 500ms |
| 3 | GET | `/api/v0/reservation/recap/{id}/audio` | 오디오 정보 조회 | 1초 | < 300ms |
| 4 | GET | `{audioUrl}` | S3 오디오 스트리밍 (7분) | - | Progressive |

#### 주요 특징
- **총 API 호출 횟수**: 3회 (ORV 서버) + 1회 (S3 직접)
- **예상 총 소요시간**: 3-5분 (오디오 청취 시간에 따라 가변)
- **스트리밍 특성**: HTTP Range Request를 통한 Progressive Download
- **병목 예상 지점**: 동시 오디오 스트리밍 시 S3 대역폭

### 2.3 시나리오 실행 패턴

#### 사용자 행동 모델
```
시나리오 A 사용자:
- 4개의 다른 스토리보드 미리보기를 탐색
- Scene을 순차적으로 진행 (인터뷰 시뮬레이션, nextSceneId 체인 따라감)
- Scene 간 50초의 답변/사고 시간 (실제 인터뷰 패턴)
- 10분 세션 내 최대 11개 Scene 진행 가능
- 영상 업로드 후 즉시 리캡 요청

시나리오 B 사용자:
- 이전에 생성된 리캡 결과 확인
- 오디오를 처음부터 끝까지 청취
- 중간에 일시정지/재개 가능 (Range Request)
```

#### 스토리보드 선택 전략 (결정론적 분배)

**사용자 ID 기반 해시 분배**:
- **6개 Scene (10%)**: 연말정산 스토리보드
- **8개 Scene (70%)**: 월요병, 오늘하루, 생일, 회고, 여행 
- **9개 Scene (20%)**: 자기소개, 짝사랑

```groovy
// TestDataProvider.groovy 구현 예시
static def getStoryboardByUserId(int userId) {
    def hash = Math.abs(userId.hashCode()) % 100
    
    if (hash < 10) {
        return "9c570f84-16b6-4c5d-85b0-eadf05829056" // 연말정산 (6개)
    } else if (hash < 80) {
        def eightSceneIds = [
            "0afecfc8-62a4-4398-85a8-0cff8b8f698f", // 월요병
            "18779df7-a80d-497c-9206-9e61540bb465", // 오늘하루
            "8c4359b2-c60a-4972-8327-89677244b12b", // 생일
            "c81d9417-5797-4b11-a8ea-c161cacfe9d1", // 회고
            "e5e9b7dc-efa4-43f9-b428-03769aabdafc"  // 여행
        ]
        return eightSceneIds[(hash - 10) % eightSceneIds.size()]
    } else {
        def nineSceneIds = [
            "8c2746c4-4613-47f8-8799-235fec7f359d", // 자기소개
            "cff1c432-b6ac-4b10-89b7-3c9be91a6699"  // 짝사랑
        ]
        return nineSceneIds[(hash - 80) % nineSceneIds.size()]
    }
}
```

#### API 호출량 재계산 (6,000명 사용자 기준 1시간당)

**시나리오 A (3,000명, 50%)**:
- 6개 Scene: 300명 × 6 = 1,800회
- 8개 Scene: 2,100명 × 8 = 16,800회  
- 9개 Scene: 600명 × 9 = 5,400회
- **Scene 조회 총합**: 24,000회

**기타 API 호출 (시나리오 A)**:
- 스토리보드 미리보기: 3,000명 × 4회 = 12,000회
- 영상 업로드: 3,000회
- 리캡 예약: 3,000회
- 영상 다운로드: 3,000회

**시나리오 B (3,000명, 50%)**:
- 리캡 결과 조회: 3,000회
- 오디오 메타데이터: 3,000회
- 오디오 스트리밍: 3,000회

| API Endpoint | 시나리오 A | 시나리오 B | 총 호출수 |
|--------------|------------|------------|-----------|
| Scene 조회 | 24,000회 | - | 24,000회 |
| 스토리보드 미리보기 | 12,000회 | - | 12,000회 |
| 영상 업로드 | 3,000회 | - | 3,000회 |
| 영상 다운로드 | 3,000회 | - | 3,000회 |
| 리캡 예약 | 3,000회 | - | 3,000회 |
| 리캡 결과 조회 | - | 3,000회 | 3,000회 |
| 오디오 메타데이터 | - | 3,000회 | 3,000회 |
| 오디오 스트리밍 | - | 3,000회 | 3,000회 |

### 2.4 부하 시나리오 구성

#### 테스트 데이터 요구사항
- **테스트 사용자**: 6,000명 (Provider: 'test')
- **스토리보드**: 기존 8개 스토리보드 활용 (월요병, 오늘하루, 자기소개, 생일, 연말정산, 회고, 짝사랑, 여행)
- **Scene**: 스토리보드별 6-9개 (실제 구조 반영), nextSceneId로 연결된 체인 구조
- **Scene 데이터 형태 (타입별 다름)**:
  - **QUESTION**: `{"question": "질문내용", "hint": "힌트", "nextSceneId": "UUID", "isHiddenQuestion": true/false}`
  - **EPILOGUE**: `{"question": "아래 문구를 따라 읽어주세요", "hint": "2025년 4월 1일 오늘은 여기까지", "nextSceneId": "UUID"}`
  - **END**: `{}` (빈 객체)
- **기존 리캡 데이터**: 사용자당 5-10개
- **테스트 영상**: 7분 분량 480p 영상 (~5MB)

#### 에러 처리 시나리오
1. **영상 업로드 실패**: 재시도 3회, 실패 시 다음 단계 진행
2. **리캡 생성 타임아웃**: 60초 대기 후 실패 처리
3. **오디오 스트리밍 중단**: 재연결 시도 1회
4. **JWT 토큰 만료**: 자동 재로그인

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
└── src/test/ngrinder/                          # nGrinder 전용 디렉토리
    ├── scenarios/
    │   ├── UserScenarioA.groovy              # 컨텐츠 생성 시나리오
    │   ├── UserScenarioB.groovy              # 컨텐츠 소비 시나리오
    │   └── MasterScenario.groovy             # 전체 부하 패턴 제어
    ├── lib/
    │   ├── TestConfig.groovy                 # 부하테스트 환경 설정
    │   ├── TestDataProvider.groovy           # 테스트 데이터 제공
    │   ├── LoadPattern.groovy                # 부하 패턴 정의
    │   ├── ApiEndpoints.groovy               # API 엔드포인트 상수
    │   ├── AuthHelper.groovy                 # JWT 인증 헬퍼
    │   ├── MediaHelper.groovy                # 미디어 업로드/다운로드 헬퍼
    │   └── MetricsCollector.groovy           # 성능 메트릭 수집
    └── resources/
        ├── test-videos/
        │   └── sample-480p-7min.mp4          # 7분 480p 테스트 영상 (~5MB)
        └── test-data/
            ├── users.json                    # 테스트 사용자 데이터
            └── storyboards.json              # 테스트 스토리보드 데이터
```

### 4.2 주요 컴포넌트 설명

#### scenarios/UserScenarioA.groovy
```groovy
// 주요 기능:
// - 로그인 및 JWT 토큰 관리
// - 병렬 API 호출 (대시보드)
// - 파일 업로드 (7분 480p 영상, ~5MB)
// - 응답 시간 측정 및 검증
```

#### scenarios/UserScenarioB.groovy
```groovy
// 주요 기능:
// - 리캡 데이터 조회
// - 오디오 스트리밍 시뮬레이션
// - 청크 단위 데이터 처리
```

#### lib/LoadPattern.groovy
```groovy
// 부하 패턴 정의:
// - Ramp-up: 10분간 0 → 86 VU
// - Peak 1: 10분간 86 VU 유지
// - Spike: 5분간 86 → 258 VU
// - Peak 2: 10분간 86 VU 유지
```

### 4.3 nGrinder 프로젝트 구성 특징
- **스크립트 언어**: 모든 스크립트는 Groovy로 작성
- **라이브러리 위치**: lib/ 디렉토리의 모든 .groovy 파일은 자동으로 classpath에 포함
- **리소스 관리**: resources/ 디렉토리의 파일들은 스크립트에서 상대경로로 접근
- **분리된 구조**: nGrinder 테스트는 메인 프로젝트와 독립적으로 src/test/ngrinder 디렉토리에서 관리

---

## 4.5 검증 결과 및 필수 보완사항

### 📅 검증 시점: 2025-07-24
### 🔍 검증 범위: API 엔드포인트, 인증 구조, DB 설정

---

### 4.5.1 발견된 이슈 및 영향도

#### 🚨 **CRITICAL (테스트 실행 불가): 2개**

##### 1. API 인증 엔드포인트 불일치
- **계획서**: `GET /api/v0/auth/callback/test`
- **실제 구현**: `GET /api/v0/auth/callback/{provider}`
- **문제**: `test` provider가 SocialAuthServiceFactory에서 지원되지 않음
- **영향**: 테스트 스크립트 실행 시 IllegalArgumentException 발생

##### 2. TestAuthService 미구현
- **현재 상태**: `test` provider 호출 시 예외 발생
- **보안 위험**: 잘못 구현 시 프로덕션에서 인증 우회 경로 생성 가능
- **영향**: 6,000명 테스트 사용자 인증 불가

#### ⚠️ **HIGH (성능 영향): 1개**

##### 3. DB Connection Pool 부족
- **현재 설정**: HikariCP 최대 10개 커넥션
- **필요 용량**: 86 VU × 평균 응답시간(0.5초) = 최소 43개 커넥션
- **부족률**: 약 4.3배 부족
- **영향**: Connection Pool 고갈로 응답 지연 및 타임아웃 에러 대량 발생 예상

#### 💡 **MEDIUM (최적화 기회): 1개**

##### 4. Scene 조회 방식 최적화 가능
- **발견**: `GET /api/v0/storyboard/{storyboardId}/scene/all` 엔드포인트 존재
- **현재 계획**: Scene을 개별적으로 6-9회 조회
- **개선 기회**: 전체 Scene을 한 번에 가져와서 클라이언트에서 순회
- **효과**: 네트워크 요청 수 85% 감소 가능

---

### 4.5.2 필수 구현사항

#### A. TestAuthService 보안 강화 구현
```java
// src/main/java/com/orv/api/domain/auth/TestAuthService.java
@Component
@ConditionalOnProperty(name = "test.auth.enabled", havingValue = "true")
@Profile({"loadtest", "test"}) // 프로덕션 환경 완전 차단
public class TestAuthService implements SocialAuthService {
    // test_user_ 접두사 강제 검증
    // 사용자 ID 범위 검증 (1-6000)
}
```

#### B. SocialAuthServiceFactory 확장
```java
// test provider 지원 추가
@Autowired(required = false) // 조건부 주입
private TestAuthService testAuthService;

public SocialAuthService getSocialAuthService(String provider) {
    // ... 기존 코드
    } else if ("test".equalsIgnoreCase(provider)) {
        if (testAuthService != null) {
            return testAuthService;
        } else {
            throw new IllegalArgumentException("테스트 인증 서비스가 활성화되지 않았습니다.");
        }
    }
}
```

#### C. DB Connection Pool 최적화
```properties
# application-loadtest.properties
# HikariCP 확장 (86 VU 동시 접속 대응)
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=20
spring.datasource.hikari.connection-timeout=10000

# JPA/Hibernate 배치 처리 최적화
spring.jpa.hibernate.jdbc.batch_size=25
spring.jpa.hibernate.order_inserts=true
```

#### D. 부하테스트 전용 프로파일 설정
```properties
# 테스트 인증 활성화
test.auth.enabled=true

# JWT 토큰 만료 시간 설정 (확인 필요)
security.jwt.expiration=720000

# Tomcat 최적화
server.tomcat.threads.max=100
server.tomcat.max-connections=200
```

---

### 4.5.3 구현 우선순위 및 검증 체크리스트

#### **구현 전 필수 검증사항**
- [ ] 실제 스토리보드 UUID 존재 여부 확인 (SQL 스크립트 사용 전)
- [ ] JWT 토큰 기본 만료 시간 확인 (application.properties 검토)
- [ ] 기존 테스트 사용자와의 social_id 충돌 가능성 검토
- [ ] PostgreSQL 데이터베이스 접근 권한 확인

#### **구현 후 필수 검증사항**
- [ ] `curl "http://localhost:8080/api/v0/auth/callback/test?code=test_user_1"` 성공 확인
- [ ] HikariCP 메트릭으로 50개 Connection Pool 동작 확인
- [ ] 테스트 데이터 정합성 검증 쿼리 실행
- [ ] loadtest 프로파일로 애플리케이션 정상 기동 확인

#### **성능 검증 기준**
- [ ] DB Connection Pool 사용률 < 80%
- [ ] 평균 응답시간 < 500ms 유지
- [ ] 에러율 < 0.1%
- [ ] 86 VU 동시 접속 시 안정성 확인

---

## 5. 구현 계획

### 5.1 Phase 0: 필수 보완사항 해결 (1일) ← **새로 추가**
- [ ] **TestAuthService 보안 강화 구현** (3시간)
  - [ ] TestAuthService 클래스 구현
  - [ ] SocialAuthServiceFactory 수정
  - [ ] 단위 테스트 작성
- [ ] **DB Connection Pool 최적화** (2시간)
  - [ ] application-loadtest.properties 생성
  - [ ] HikariCP 설정 최적화
  - [ ] JPA/Hibernate 배치 처리 설정
- [ ] **테스트 데이터 스크립트 준비** (2시간)
  - [ ] 6,000명 사용자 생성 SQL
  - [ ] 기존 리캡 데이터 생성 SQL
  - [ ] 데이터 정리 스크립트
- [ ] **통합 검증** (1시간)
  - [ ] loadtest 프로파일 빌드 테스트
  - [ ] 테스트 인증 동작 확인

### 5.2 Phase 1: 기초 환경 구축 (2일) ← **1일 단축**
- [ ] nGrinder 디렉토리 구조 생성
- [ ] Gradle 의존성 추가
- [ ] 기본 헬퍼 클래스 구현
- [ ] API 엔드포인트 매핑

### 5.3 Phase 2: 테스트 데이터 준비 (1일) ← **Phase 0에서 기반 작업 완료로 단축**
- [ ] **기존 8개 스토리보드 검증** 
  - [ ] Scene 체인 무결성 확인
  - [ ] Scene 타입별 content 구조 검증
  - [ ] **실제 스토리보드 UUID 존재 여부 확인** ← **추가**
- [ ] **TestDataProvider 클래스 구현**
  - [ ] 사용자 ID 기반 해시 분배 로직
  - [ ] 스토리보드별 Scene 개수 매핑
  - [ ] **Scene 전체 조회 최적화 반영** ← **추가**
- [ ] **테스트 영상 및 S3 설정**
  - [ ] 7분 480p 테스트 영상 준비 (~5MB)
  - [ ] S3 업로드 스크립트 작성
  - [ ] CloudFront 배포 확인

### 5.4 Phase 3: nGrinder 스크립트 개발 (3일)
- [ ] UserScenarioA 구현
  - [ ] 로그인 로직
  - [ ] **사용자별 고정 스토리보드 선택** ← **수정**
  - [ ] 대시보드 조회 (병렬 처리)
  - [ ] Scene 순차 조회 (실제 nextSceneId 체인 따라가기)
  - [ ] 50초 딜레이 시뮬레이션 및 세션 시간 차별화 (6개→8분, 8개→10분, 9개→11분)
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

#### 1. 실제 DB 스키마 기반 TestAuthService 구현

##### 1.1 TestAuthService 클래스
```java
// src/main/java/com/orv/api/domain/auth/TestAuthService.java
@Component
@ConditionalOnProperty(name = "test.auth.enabled", havingValue = "true")
public class TestAuthService implements SocialAuthService {
    
    @Override
    public String getAuthorizationUrl(String state) {
        // nGrinder에서는 실제 OAuth 플로우 없이 바로 callback 호출
        return "http://localhost:8080/test-oauth-callback";
    }
    
    @Override
    public SocialUserInfo getUserInfo(String code) {
        // code 형식: "test_user_1", "test_user_2", ..., "test_user_6000"
        if (!code.startsWith("test_user_")) {
            throw new IllegalArgumentException("Invalid test auth code: " + code);
        }
        
        String userId = code.replace("test_user_", "");
        
        return SocialUserInfo.builder()
            .provider("test")
            .id("fake_social_id_" + userId)           // social_id (UNIQUE)
            .email("loadtest_" + userId + "@test.com")
            .name("LoadTest" + userId)
            .build();
    }
}
```

##### 1.2 SocialAuthServiceFactory 수정
```java
// src/main/java/com/orv/api/domain/auth/SocialAuthServiceFactory.java
@Service
@RequiredArgsConstructor
public class SocialAuthServiceFactory {
    private final GoogleAuthService googleAuthService;
    private final KakaoAuthService kakaoAuthService;
    private final Optional<TestAuthService> testAuthService; // Optional로 처리
    
    public SocialAuthService getSocialAuthService(String provider) {
        if ("google".equalsIgnoreCase(provider)) {
            return googleAuthService;
        } else if ("kakao".equalsIgnoreCase(provider)) {
            return kakaoAuthService;
        } else if ("test".equalsIgnoreCase(provider) && testAuthService.isPresent()) {
            return testAuthService.get();
        } else {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다: " + provider);
        }
    }
}
```

#### 2. 실제 DB 스키마 기반 테스트 데이터 생성

##### 2.1 기본 테스트 사용자 생성 (6,000명)
```sql
-- 1. 기본 역할 생성 (없다면)
INSERT INTO role (id, name) 
VALUES (uuid_generate_v4(), 'USER')
ON CONFLICT DO NOTHING;

-- 2. 6,000명의 테스트 사용자 생성 (실제 스키마 제약사항 반영)
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
    uuid_generate_v4(),                           -- member 테이블은 uuid_generate_v4() 사용
    'LT' || LPAD(generate_series::text, 6, '0'),  -- LT000001, LT000002, ... (8자 제한)
    'test',
    'fake_social_id_' || generate_series,         -- social_id (provider, social_id 조합 UNIQUE)
    'loadtest_' || generate_series || '@test.com',
    'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-profile.png',
    'LoadTest' || generate_series,
    CASE WHEN generate_series % 2 = 0 THEN 'MALE' ELSE 'FEMALE' END,
    '010-' || LPAD((generate_series % 10000)::text, 4, '0') || '-' || LPAD((generate_series % 10000)::text, 4, '0'),
    CURRENT_TIMESTAMP - INTERVAL '1 day' * (random() * 365)  -- 지난 1년 내 랜덤 가입일
FROM generate_series(1, 6000);

-- 3. 모든 테스트 사용자에게 USER 권한 부여
INSERT INTO member_role (member_id, role_id)
SELECT 
    m.id,
    r.id
FROM member m, role r
WHERE m.provider = 'test' 
  AND r.name = 'USER';
```

##### 2.2 시나리오 B용 기존 리캡 데이터 생성

###### 2.2.1 테스트 영상 데이터 생성
```sql
-- 각 테스트 사용자마다 1-3개의 과거 영상 생성
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
    uuid_generate_v4(),                           -- video 테이블은 uuid_generate_v4() 사용
    -- 기존 스토리보드 중 랜덤 선택
    (ARRAY[
        'E5E9B7DC-EFA4-43F9-B428-03769AABDAFC',
        'C81D9417-5797-4B11-A8EA-C161CACFE9D1', 
        'CFF1C432-B6AC-4B10-89B7-3C9BE91A6699',
        '8c2746c4-4613-47f8-8799-235fec7f359d'
    ])[ceil(random() * 4)::int]::uuid,
    m.id,
    'https://test-bucket.s3.amazonaws.com/videos/' || m.id || '/' || video_num || '.mp4',
    'https://test-bucket.s3.amazonaws.com/thumbnails/' || m.id || '/' || video_num || '.jpg',
    'Test Video ' || video_num,
    CURRENT_TIMESTAMP - INTERVAL '1 day' * (random() * 60)  -- 지난 2달 내
FROM 
    member m,
    generate_series(1, 1 + floor(random() * 3)::int) as video_num  -- 1-3개 랜덤
WHERE m.provider = 'test';
```

###### 2.2.2 리캡 예약 및 결과 데이터 생성
```sql
-- interview_audio_recording 테이블에 오디오 녹음 데이터 생성
-- 각 비디오에 대해 하나의 오디오 녹음 생성
WITH video_audio_pairs AS (
    SELECT 
        uuid_generate_v4() as audio_id,
        v.id as video_id,
        v.storyboard_id,
        v.member_id,
        v.created_at
    FROM video v
    WHERE EXISTS (SELECT 1 FROM member m WHERE m.id = v.member_id AND m.provider = 'test')
)
INSERT INTO interview_audio_recording (
    id,
    storyboard_id,
    member_id,
    video_url,
    running_time,
    created_at
)
SELECT 
    audio_id,
    storyboard_id,
    member_id,
    'https://test-bucket.s3.amazonaws.com/audio/' || video_id || '.mp3',
    420 + floor(random() * 180)::int,  -- 7-10분 (420-600초)
    created_at + INTERVAL '5 minutes'
FROM video_audio_pairs;

-- recap_result 테이블에 리캡 결과 생성
WITH audio_result_pairs AS (
    SELECT 
        iar.id as audio_id,
        gen_random_uuid() as result_id,
        iar.created_at + INTERVAL '2 hours' as result_created_at
    FROM interview_audio_recording iar
    WHERE EXISTS (
        SELECT 1 FROM member m 
        WHERE m.id = iar.member_id AND m.provider = 'test'
    )
)
INSERT INTO recap_result (id, created_at)
SELECT result_id, result_created_at
FROM audio_result_pairs;

-- recap_reservation 테이블에 완료된 리캡 예약 생성
WITH reservation_data AS (
    SELECT 
        uuid_generate_v4() as reservation_id,
        v.member_id,
        v.id as video_id,
        iar.id as audio_id,
        rr.id as result_id,
        iar.created_at as scheduled_at,
        iar.created_at - INTERVAL '30 minutes' as reservation_created_at
    FROM video v
    JOIN interview_audio_recording iar 
        ON iar.member_id = v.member_id 
        AND iar.storyboard_id = v.storyboard_id
    JOIN recap_result rr 
        ON rr.created_at = iar.created_at + INTERVAL '2 hours'  -- 정확한 시간 매칭
    WHERE EXISTS (SELECT 1 FROM member m WHERE m.id = v.member_id AND m.provider = 'test')
)
INSERT INTO recap_reservation (
    id,
    member_id,
    video_id,
    interview_audio_recording_id,
    recap_result_id,
    scheduled_at,
    created_at
)
SELECT 
    reservation_id,
    member_id,
    video_id,
    audio_id,
    result_id,
    scheduled_at,
    reservation_created_at
FROM reservation_data;

-- recap_answer_summary 테이블에 리캡 답변 요약 생성 (scene별)
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
    rr.recap_result_id,
    s.id,
    '테스트용 Scene ' || ROW_NUMBER() OVER (PARTITION BY rr.recap_result_id ORDER BY s.id) || ' 답변 요약입니다.',
    ROW_NUMBER() OVER (PARTITION BY rr.recap_result_id ORDER BY s.id),
    rr.created_at
FROM recap_reservation rr
JOIN video v ON v.id = rr.video_id
JOIN scene s ON s.storyboard_id = v.storyboard_id
JOIN recap_result result ON result.id = rr.recap_result_id
WHERE EXISTS (
    SELECT 1 FROM member m 
    WHERE m.id = rr.member_id AND m.provider = 'test'
);
```

#### 3. nGrinder AuthHelper 구체적 구현

##### 3.1 AuthHelper 클래스
```groovy
// src/test/ngrinder/lib/AuthHelper.groovy
class AuthHelper {
    static String authenticateTestUser(HTTPRequest request, int userId) {
        def baseUrl = TestConfig.API_BASE_URL
        def authCode = "test_user_${userId}"
        
        // OAuth 콜백 시뮬레이션
        def response = request.GET("${baseUrl}/api/v0/auth/callback/test?code=${authCode}")
        
        // 리다이렉트 URL에서 JWT 토큰 추출
        def location = response.getHeader("Location")
        def tokenMatch = location =~ /jwtToken=([^&]+)/
        
        if (tokenMatch) {
            return tokenMatch[0][1]
        } else {
            throw new RuntimeException("Failed to extract JWT token from: ${location}")
        }
    }
    
    static Map<String, String> getAuthHeaders(String jwtToken) {
        return ["Authorization": "Bearer ${jwtToken}"]
    }
    
    static boolean isTokenExpired(HTTPResponse response) {
        return response.statusCode == 401
    }
    
    static String refreshToken(HTTPRequest request, int userId) {
        // 토큰 만료 시 재인증
        return authenticateTestUser(request, userId)
    }
}
```

##### 3.2 UserScenario에서 사용 예시
```groovy
// UserScenarioA.groovy에서 사용법
@Test
public void testScenarioA() {
    // 1. 로그인 시뮬레이션
    def userId = ThreadLocalRandom.current().nextInt(1, 6001)
    def token = AuthHelper.authenticateTestUser(request, userId)
    def headers = AuthHelper.getAuthHeaders(token)
    
    // 2. API 호출 시 토큰 사용
    def response = request.GET("${baseUrl}/api/v0/topic/list", headers)
    
    // 3. 토큰 만료 시 재인증
    if (AuthHelper.isTokenExpired(response)) {
        token = AuthHelper.refreshToken(request, userId)
        headers = AuthHelper.getAuthHeaders(token)
        response = request.GET("${baseUrl}/api/v0/topic/list", headers)
    }
    
    // 이후 모든 API 호출에 headers 사용
}
```

#### 4. 환경 설정 및 보안

##### 4.1 Application Properties
```properties
# application-loadtest.properties
test.auth.enabled=true
security.frontend.callback-url=http://localhost:3000/auth/callback

# 로깅 설정 (테스트 시 디버깅용)
logging.level.com.orv.api.domain.auth=DEBUG
logging.level.com.orv.api.domain.reservation=DEBUG

# 테스트 환경 최적화
spring.jpa.show-sql=false
spring.jpa.hibernate.ddl-auto=none
```

##### 4.2 안전장치
- **환경 분리**: `@ConditionalOnProperty`로 테스트 환경에서만 활성화
- **데이터 격리**: provider='test'로 구분
- **자동 정리**: 테스트 후 자동 정리 스크립트

#### 5. 테스트 데이터 정리 스크립트

##### 5.1 외래키 제약사항 고려한 순서별 삭제
```sql
-- 테스트 데이터 정리 스크립트 (의존성 순서 고려)
-- 1. recap_answer_summary 삭제
DELETE FROM recap_answer_summary WHERE recap_result_id IN (
    SELECT DISTINCT rr.recap_result_id 
    FROM recap_reservation rr 
    JOIN member m ON m.id = rr.member_id 
    WHERE m.provider = 'test'
);

-- 2. recap_reservation 삭제
DELETE FROM recap_reservation WHERE member_id IN (
    SELECT id FROM member WHERE provider = 'test'
);

-- 3. recap_result 삭제 (고아 레코드)
DELETE FROM recap_result WHERE id NOT IN (
    SELECT DISTINCT recap_result_id 
    FROM recap_reservation 
    WHERE recap_result_id IS NOT NULL
);

-- 4. interview_audio_recording 삭제
DELETE FROM interview_audio_recording WHERE member_id IN (
    SELECT id FROM member WHERE provider = 'test'
);

-- 5. video 삭제
DELETE FROM video WHERE member_id IN (
    SELECT id FROM member WHERE provider = 'test'
);

-- 6. member_role 삭제
DELETE FROM member_role WHERE member_id IN (
    SELECT id FROM member WHERE provider = 'test'
);

-- 7. member 삭제
DELETE FROM member WHERE provider = 'test';

-- 8. 통계 정보 업데이트
ANALYZE member;
ANALYZE video;
ANALYZE recap_reservation;
```

##### 5.2 정리 스크립트 자동화
```bash
#!/bin/bash
# cleanup-test-data.sh
echo "테스트 데이터 정리 시작..."

psql -h $DB_HOST -U $DB_USER -d $DB_NAME << EOF
-- 정리 스크립트 실행
\i cleanup-test-data.sql
EOF

echo "테스트 데이터 정리 완료!"
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
