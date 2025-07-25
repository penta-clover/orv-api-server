# 5. 구현 계획

> **[← 이전: 프로젝트 구조](04-project-structure.md)** | **[메인으로 돌아가기](README.md)** | **[다음: 인프라 구성 →](06-infrastructure.md)**

## 5.1 Phase 0: 필수 보완사항 해결 (1일) ← **새로 추가**

### 🚨 Critical 이슈 해결 우선
검증 결과에서 발견된 테스트 실행 불가 이슈들을 최우선으로 해결합니다.

#### A. TestAuthService 보안 강화 구현 (3시간)
- [ ] **TestAuthService 클래스 구현**
  - [ ] SocialAuthService 인터페이스 구현
  - [ ] test_user_ 접두사 강제 검증
  - [ ] 사용자 ID 범위 검증 (1-6000)
  - [ ] @ConditionalOnProperty 및 @Profile 적용
- [ ] **SocialAuthServiceFactory 수정**
  - [ ] test provider 지원 추가
  - [ ] Optional 타입으로 조건부 주입
  - [ ] 예외 처리 로직 추가
- [ ] **단위 테스트 작성**
  - [ ] TestAuthService 동작 확인
  - [ ] 보안 검증 로직 테스트

#### B. DB Connection Pool 최적화 (2시간)
- [ ] **application-loadtest.properties 생성**
  - [ ] HikariCP 설정: maximum-pool-size=50
  - [ ] HikariCP 설정: minimum-idle=20
  - [ ] connection-timeout=10000
- [ ] **JPA/Hibernate 배치 처리 설정**
  - [ ] batch_size=25
  - [ ] order_inserts=true
- [ ] **Tomcat 최적화**
  - [ ] threads.max=100
  - [ ] max-connections=200

#### C. 테스트 데이터 스크립트 준비 (2시간)
- [ ] **6,000명 사용자 생성 SQL**
  - [ ] member 테이블 대량 삽입
  - [ ] member_role 관계 설정
  - [ ] UUID 및 제약사항 검증
- [ ] **기존 리캡 데이터 생성 SQL**
  - [ ] video, interview_audio_recording
  - [ ] recap_result, recap_reservation
  - [ ] recap_answer_summary
- [ ] **데이터 정리 스크립트**
  - [ ] 외래키 순서 고려한 삭제
  - [ ] 자동화 스크립트 작성

#### D. 통합 검증 (1시간)
- [ ] **loadtest 프로파일 빌드 테스트**
  - [ ] 애플리케이션 정상 기동 확인
  - [ ] 의존성 주입 오류 없음 확인
- [ ] **테스트 인증 동작 확인**
  - [ ] curl 테스트 실행
  - [ ] JWT 토큰 정상 발급 확인

---

## 5.2 Phase 1: 기초 환경 구축 (2일) ← **1일 단축**

Phase 0에서 기반 작업이 완료되어 단축된 일정으로 진행합니다.

### Day 1: nGrinder 프로젝트 설정
- [ ] **nGrinder 디렉토리 구조 생성**
  ```bash
  mkdir -p src/test/ngrinder/{scenarios,lib,resources/test-videos,resources/test-data}
  ```
- [ ] **Gradle 의존성 추가**
  - [ ] nGrinder 관련 의존성
  - [ ] Groovy 컴파일 설정
  - [ ] 테스트 리소스 경로 설정
- [ ] **기본 설정 파일 생성**
  - [ ] TestConfig.groovy
  - [ ] ApiEndpoints.groovy

### Day 2: 헬퍼 클래스 구현
- [ ] **AuthHelper.groovy**
  - [ ] JWT 토큰 인증 로직
  - [ ] 토큰 만료 처리
  - [ ] 재인증 메커니즘
- [ ] **MediaHelper.groovy**
  - [ ] 파일 업로드 처리
  - [ ] 파일 다운로드 처리
  - [ ] multipart 요청 생성
- [ ] **MetricsCollector.groovy**
  - [ ] 성능 메트릭 수집
  - [ ] 로그 기록 기능

---

## 5.3 Phase 2: 테스트 데이터 준비 (1일) ← **Phase 0에서 기반 작업 완료로 단축**

### 기존 8개 스토리보드 검증
- [ ] **Scene 체인 무결성 확인**
  - [ ] nextSceneId 연결 검증
  - [ ] 순환 참조 여부 확인
  - [ ] END 타입 Scene 존재 확인
- [ ] **Scene 타입별 content 구조 검증**
  - [ ] QUESTION 타입 필드 확인
  - [ ] EPILOGUE 타입 필드 확인
  - [ ] END 타입 빈 객체 확인
- [ ] **실제 스토리보드 UUID 존재 여부 확인** ← **추가**
  - [ ] DB에서 실제 UUID 조회
  - [ ] 계획서의 UUID와 실제 매칭 확인

### TestDataProvider 클래스 구현
- [ ] **사용자 ID 기반 해시 분배 로직**
  - [ ] 6개 Scene (10%): 연말정산
  - [ ] 8개 Scene (70%): 월요병, 오늘하루, 생일, 회고, 여행
  - [ ] 9개 Scene (20%): 자기소개, 짝사랑
- [ ] **스토리보드별 Scene 개수 매핑**
  - [ ] 실제 DB 스키마 기반 매핑
  - [ ] Scene 순서 정보 포함
- [ ] **Scene 전체 조회 최적화 반영** ← **추가**
  - [ ] `/scene/all` 엔드포인트 활용
  - [ ] 개별 Scene 조회 vs 전체 조회 선택 로직

### 테스트 영상 및 S3 설정
- [ ] **7분 480p 테스트 영상 준비 (~5MB)**
  - [ ] 실제 영상 파일 생성 또는 준비
  - [ ] nGrinder resources 디렉토리 배치
- [ ] **S3 업로드 스크립트 작성**
  - [ ] AWS CLI 또는 SDK 사용
  - [ ] 배치 업로드 스크립트
- [ ] **CloudFront 배포 확인**
  - [ ] 캐시 설정 확인
  - [ ] 접근 권한 검증

---

## 5.4 Phase 3: nGrinder 스크립트 개발 (3일)

### Day 1-2: UserScenarioA 구현
- [ ] **로그인 로직**
  - [ ] AuthHelper를 활용한 테스트 인증
  - [ ] JWT 토큰 추출 및 저장
  - [ ] 토큰 만료 시 재인증
- [ ] **사용자별 고정 스토리보드 선택** ← **수정**
  - [ ] TestDataProvider.getStoryboardByUserId() 활용
  - [ ] 해시 기반 결정론적 분배
  - [ ] 스토리보드별 Scene 개수 확인
- [ ] **대시보드 조회 (병렬 처리)**
  - [ ] topic/list, archive/videos/my 동시 호출
  - [ ] 스레드 안전성 확보
  - [ ] 응답 시간 측정
- [ ] **Scene 순차 조회 (실제 nextSceneId 체인 따라가기)**
  - [ ] 첫 번째 Scene부터 시작
  - [ ] nextSceneId로 다음 Scene 조회
  - [ ] END 타입까지 순차 진행
- [ ] **50초 딜레이 시뮬레이션 및 세션 시간 차별화**
  - [ ] 6개 Scene → 8분 총 세션
  - [ ] 8개 Scene → 10분 총 세션  
  - [ ] 9개 Scene → 11분 총 세션
- [ ] **영상 업로드 (multipart)**
  - [ ] MediaHelper 활용
  - [ ] 7분 480p 영상 (~5MB) 업로드
  - [ ] 업로드 실패 시 재시도 로직
- [ ] **리캡 예약**
  - [ ] 업로드된 video_id 사용
  - [ ] 예약 생성 API 호출

### Day 2-3: UserScenarioB 구현
- [ ] **리캡 조회**
  - [ ] 기존 recap_reservation 데이터 활용
  - [ ] 사용자별 리캡 결과 조회
  - [ ] 오디오 메타데이터 획득
- [ ] **오디오 스트리밍**
  - [ ] S3 직접 접근
  - [ ] HTTP Range Request 시뮬레이션
  - [ ] Progressive Download 구현
  - [ ] 7분간 스트리밍 유지

### Day 3: MasterScenario 구현
- [ ] **VU 분배 로직**
  - [ ] 시나리오 A: 50% (43 VU)
  - [ ] 시나리오 B: 50% (43 VU)
  - [ ] 동적 분배 알고리즘
- [ ] **부하 패턴 제어**
  - [ ] LoadPattern.groovy 연동
  - [ ] 35분 전체 시나리오 제어
  - [ ] Phase별 VU 증감 관리

---

## 5.5 Phase 4: 테스트 실행 및 분석 (2일)

### Day 1: 파일럿 테스트
- [ ] **10 VU 소규모 테스트**
  - [ ] 스크립트 동작 검증
  - [ ] 기본 기능 확인
  - [ ] 에러 로그 분석
- [ ] **문제점 수정**
  - [ ] 발견된 버그 수정
  - [ ] 성능 이슈 개선
  - [ ] 설정값 튜닝

### Day 2: 본 테스트 실행
- [ ] **86 VU 본격 테스트**
  - [ ] 35분 풀 시나리오 실행
  - [ ] 실시간 모니터링
  - [ ] 로그 수집 및 저장
- [ ] **결과 분석 및 리포트 작성**
  - [ ] nGrinder 리포트 생성
  - [ ] CloudWatch 메트릭 분석
  - [ ] 병목 지점 식별
  - [ ] 개선 사항 도출

---

## 📊 Phase별 진행 현황 추적

### 체크리스트 템플릿

| Phase | 주요 작업 | 예상 시간 | 완료율 | 상태 |
|-------|----------|-----------|--------|------|
| **Phase 0** | 필수 보완사항 해결 | 8시간 | 0% | ⏳ 대기 |
| **Phase 1** | 기초 환경 구축 | 16시간 | 0% | ⏳ 대기 |
| **Phase 2** | 테스트 데이터 준비 | 8시간 | 0% | ⏳ 대기 |
| **Phase 3** | nGrinder 스크립트 개발 | 24시간 | 0% | ⏳ 대기 |
| **Phase 4** | 테스트 실행 및 분석 | 16시간 | 0% | ⏳ 대기 |

### 리스크 관리

| 리스크 | 영향도 | 대응 방안 | 담당자 |
|--------|--------|-----------|--------|
| TestAuthService 보안 이슈 | HIGH | Phase 0에서 최우선 해결 | 개발팀 |
| DB Connection Pool 부족 | HIGH | 설정 최적화로 해결 | 인프라팀 |
| 실제 스토리보드 UUID 불일치 | MEDIUM | Phase 2에서 검증 후 수정 | 개발팀 |
| S3 대역폭 제한 | MEDIUM | CloudFront 최적화 | 인프라팀 |

## 📋 관련 문서

- **이전 단계**: [프로젝트 구조](04-project-structure.md)에서 검증 결과 및 필수 보완사항 확인
- **다음 단계**: [인프라 구성](06-infrastructure.md)에서 AWS 환경 설정 확인
- **구현 참조**: [테스트 데이터 생성 가이드](appendix-test-data-guide.md)에서 상세 구현 방법 확인

---

**[← 이전: 프로젝트 구조](04-project-structure.md)** | **[메인으로 돌아가기](README.md)** | **[다음: 인프라 구성 →](06-infrastructure.md)**
