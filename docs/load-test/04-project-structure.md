# 4. 프로젝트 구조

> **[← 이전: 부하 분석 및 목표 설정](03-performance-targets.md)** | **[메인으로 돌아가기](README.md)** | **[다음: 구현 계획 →](05-implementation-plan.md)**

## 4.1 디렉토리 구조

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

## 4.2 주요 컴포넌트 설명

### scenarios/UserScenarioA.groovy
```groovy
// 주요 기능:
// - 로그인 및 JWT 토큰 관리
// - 병렬 API 호출 (대시보드)
// - 파일 업로드 (7분 480p 영상, ~5MB)
// - 응답 시간 측정 및 검증
```

### scenarios/UserScenarioB.groovy
```groovy
// 주요 기능:
// - 리캡 데이터 조회
// - 오디오 스트리밍 시뮬레이션
// - 청크 단위 데이터 처리
```

### lib/LoadPattern.groovy
```groovy
// 부하 패턴 정의:
// - Ramp-up: 10분간 0 → 86 VU
// - Peak 1: 10분간 86 VU 유지
// - Spike: 5분간 86 → 258 VU
// - Peak 2: 10분간 86 VU 유지
```

## 4.3 nGrinder 프로젝트 구성 특징

- **스크립트 언어**: 모든 스크립트는 Groovy로 작성
- **라이브러리 위치**: lib/ 디렉토리의 모든 .groovy 파일은 자동으로 classpath에 포함
- **리소스 관리**: resources/ 디렉토리의 파일들은 스크립트에서 상대경로로 접근
- **분리된 구조**: nGrinder 테스트는 메인 프로젝트와 독립적으로 src/test/ngrinder 디렉토리에서 관리

---

## 4.4 검증 결과 및 필수 보완사항

### 📅 검증 시점: 2025-07-24
### 🔍 검증 범위: API 엔드포인트, 인증 구조, DB 설정

---

### 4.4.1 발견된 이슈 및 영향도

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

### 4.4.2 필수 구현사항

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

### 4.4.3 구현 우선순위 및 검증 체크리스트

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

## 📋 관련 문서

- **이전 단계**: [부하 분석 및 목표 설정](03-performance-targets.md)에서 성능 목표 확인
- **다음 단계**: [구현 계획](05-implementation-plan.md)에서 단계별 구현 방법 확인
- **구현 가이드**: [테스트 데이터 생성 가이드](appendix-test-data-guide.md)에서 상세 구현 방법 확인

---

**[← 이전: 부하 분석 및 목표 설정](03-performance-targets.md)** | **[메인으로 돌아가기](README.md)** | **[다음: 구현 계획 →](05-implementation-plan.md)**
