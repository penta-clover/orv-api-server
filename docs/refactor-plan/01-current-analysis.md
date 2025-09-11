# 현재 상태 분석

## 📊 기술 스택 현황

### Backend
- **Framework**: Spring Boot 3.x
- **언어**: Java 17+
- **빌드 도구**: Gradle
- **데이터베이스**: PostgreSQL
- **ORM**: JDBC Template (JPA 미사용)
- **인증**: JWT Token
- **파일 저장소**: AWS S3
- **마이그레이션**: Flyway

### Infrastructure
- **컨테이너**: Docker
- **CDN**: AWS CloudFront
- **로컬 개발**: LocalStack (S3 에뮬레이션)

## 🏗 아키텍처 구조

### 레이어드 아키텍처
```
├── Controller Layer
│   └── REST API endpoints
├── Service Layer
│   └── Business Logic
├── Repository Layer
│   ├── JDBC Implementation
│   ├── S3 Implementation
│   └── Memory Implementation (테스트용)
└── Domain Model
    └── DTO & Entity
```

### 패키지 구조
```
com.orv.api/
├── domain/
│   ├── admin/
│   ├── archive/    # 영상/이미지 관리
│   ├── auth/       # 인증/인가
│   ├── health/     # 헬스체크
│   ├── media/      # 미디어 처리
│   ├── recap/      # 요약 서비스
│   ├── reservation/# 예약 시스템
│   ├── storyboard/ # 스토리보드
│   └── term/       # 약관 관리
├── global/
│   ├── bizgo/      # 외부 서비스 연동
│   ├── dto/        # 공통 DTO
│   └── error/      # 에러 처리
└── 설정 클래스들
```

## 🔍 주요 도메인 분석

### 1. Archive (영상 아카이브)
- **기능**: 영상 업로드, 썸네일 관리, 메타데이터 저장
- **저장소**: S3 + PostgreSQL (메타데이터)
- **이슈**: 
  - Controller에 비즈니스 로직 존재 (`calculateRunningTime`)
  - 파일 타입 검증 부재
  - 대용량 파일 동기 처리

### 2. Auth (인증/인가)
- **기능**: 소셜 로그인, JWT 토큰 발급, 회원 관리
- **구현**: Spring Security + JWT
- **이슈**:
  - CORS 설정이 모든 origin 허용
  - JWT secret 환경변수로만 관리

### 3. Reservation (예약 시스템)
- **기능**: 인터뷰 예약, 즉시 예약, 예약 상태 관리
- **구현**: Quartz 스케줄러 사용
- **특징**: 시간대 처리 (ZonedDateTime 사용)

### 4. Storyboard (스토리보드)
- **기능**: 스토리보드 생성/조회, 씬 관리, 토픽 연결
- **구현**: 계층적 데이터 구조
- **특징**: 카테고리, 해시태그 지원

## 🚨 주요 문제점 요약

### 보안
1. **CORS 설정**: `setAllowedOriginPatterns(List.of("*"))`
2. **파일 업로드**: 검증 없이 모든 파일 허용
3. **시크릿 관리**: 환경변수로만 관리 (하드코딩된 CloudFront 도메인)
4. **에러 처리**: `printStackTrace()` 사용으로 스택 트레이스 노출 위험

### 아키텍처
1. **Repository 패턴 불일치**: Memory/Jdbc/S3 구현체가 통일되지 않음
2. **Service 인터페이스**: 일부만 인터페이스 존재 (ReservationService)
3. **비즈니스 로직 위치**: Controller에 존재하는 경우 있음
4. **JDBC Template**: JPA 미사용으로 보일러플레이트 코드 과다

### 코드 품질
1. **에러 처리**: 모든 예외를 500 에러로 처리
2. **로깅**: 로그 레벨 부적절 (warn, error 구분 미흡)
3. **매직 넘버**: 하드코딩된 값들 (100, 500 등)
4. **코드 중복**: MemberInfo vs MemberProfile 등

### 성능
1. **캐싱 전략 부재**: Redis 등 캐시 미사용
2. **동기 처리**: 대용량 파일 업로드 시 블로킹
3. **N+1 쿼리**: JPA 미사용으로 최적화 어려움
4. **인덱스 전략**: 데이터베이스 인덱스 설계 불명확

### 테스트
1. **테스트 커버리지**: 측정 불가 (설정 없음)
2. **통합 테스트**: 부족한 상태
3. **E2E 테스트**: 없음
4. **Mocking**: 일부 테스트만 존재

## ✅ 잘 되어있는 부분

1. **DB 마이그레이션**: Flyway를 통한 체계적 관리
2. **도메인 분리**: 패키지 구조가 도메인별로 잘 나뉨
3. **DTO 패턴**: Request/Response DTO 분리
4. **REST Docs**: API 문서 자동화
5. **부하 테스트**: nGrinder 스크립트 존재

## 📌 개선 우선순위

1. **긴급**: 보안 취약점 (CORS, 파일 업로드, 에러 처리)
2. **높음**: 아키텍처 정리 (Repository/Service 패턴)
3. **중간**: 성능 최적화 (캐싱, 비동기 처리)
4. **낮음**: 코드 품질 개선 (리팩토링, 테스트 추가)

---

다음: [보안 이슈 및 개선방안](./02-security-issues.md) →
