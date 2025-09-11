# ORV API Server 부하테스트 실행 가이드

## 🎯 현재 상황 요약

### ✅ 완료된 작업 (100%)
- **Phase 0**: TestAuthService, 테스트 데이터 SQL, 자동화 스크립트 완성
- **nGrinder 라이브러리**: 5개 핵심 클래스 완벽 구현
- **시나리오 스크립트**: A(비디오 업로드), B(리캡 스트리밍) 완성
- **성능 목표**: 86명 동시사용자, 500ms 응답시간, 0.1% 에러율

---

## 🚀 실행 순서

### 1단계: 로컬 환경 준비

#### 옵션 A: Docker 환경 (추천)
```bash
# PostgreSQL + API 서버 실행
cd docker/dev
docker-compose up -d

# 상태 확인
docker-compose ps
```

#### 옵션 B: 로컬 환경
```bash
# PostgreSQL 실행 (별도)
# API 서버 실행
./gradlew bootRun --args='--spring.profiles.active=loadtest'
```

### 2단계: 테스트 데이터 생성 (Phase 0)

#### 옵션 A: Warm-up 스크립트 사용 (권장) 🆕
```bash
# nGrinder를 통한 사용자 생성
# 파일: src/test/load/scenarios/scenario_a001.groovy/warm_up.groovy

nGrinder 설정:
- Script: warm_up.groovy
- vUser: 50
- Duration: Run Count 1
- 예상 시간: 10-15분

# 생성되는 사용자:
# test_user_0_0_0 ~ test_user_5_9_99 (총 6,000명)
# agent: 0-5, process: 0-9, thread: 0-99
```

#### 옵션 B: SQL 스크립트 사용 (기존 방식)
```bash
# 환경변수 설정 (필요시)
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=orv_api
export DB_USER=postgres
export DB_PASSWORD=password

# 테스트 데이터 자동 생성
./src/test/resources/load-test/setup_load_test.sh

# 결과 확인 (예상)
# ✓ 6,000명 테스트 사용자 생성
# ✓ 42,000개 리캡 데이터 생성
# ✓ TestAuthService 동작 확인
```

> **참고**: Warm-up 방식은 실제 API를 통해 사용자를 생성하므로 더 현실적인 테스트가 가능합니다.

### 3단계: nGrinder 부하테스트 실행

#### 🎬 시나리오 A: 비디오 업로드
```
파일: src/test/ngrinder/scenario_a_video_upload.groovy

테스트 플로우:
1. TestAuthService 인증
2. 토픽 목록 + 내 아카이브 조회 (병렬)
3. 사용자별 고정 스토리보드 선택
4. Scene별 순차 조회 (50초 딜레이)
5. 비디오 업로드 (7분 480p, ~5MB)
6. 리캡 예약 생성

성능 목표:
- 동시 사용자: 86명
- 세션 시간: 8-11분 (스토리보드별 차별화)
- 응답시간: < 500ms
- 에러율: < 0.1%
```

#### 🎵 시나리오 B: 리캡 스트리밍
```
파일: src/test/ngrinder/scenario_b_recap_streaming.groovy

테스트 플로우:
1. TestAuthService 인증
2. 내 리캡 목록 조회
3. 기존 리캡 결과 상세 조회
4. 리캡 오디오 URL 조회
5. 7분간 Progressive Download 오디오 스트리밍

성능 목표:
- 동시 사용자: 86명
- 세션 시간: 7분
- 응답시간: < 500ms
- 에러율: < 0.1%
```

---

## 📊 nGrinder 설정 가이드

### 기본 설정
```
Target Hosts: localhost:8080 (또는 테스트 서버)
Script: scenario_a_video_upload.groovy 또는 scenario_b_recap_streaming.groovy
Agent: 1개 (로컬)
```

### 부하 패턴 설정
```
Virtual Users: 86명
Ramp-up Time: 10분
Peak Duration: 10분
Test Duration: 30분 (총)

분배:
- 시나리오 A: 43명 (50%)
- 시나리오 B: 43명 (50%)
```

### 고급 설정
```
Connection Timeout: 10초
Read Timeout: 30초
Max Request Per Second: 제한 없음
Think Time: 계산된 딜레이 사용
```

---

## 🗂️ 프로젝트 구조

```
src/test/ngrinder/
├── lib/                           # 라이브러리 클래스
│   ├── TestConfig.groovy         # 모든 설정값, 성능 목표
│   ├── ApiEndpoints.groovy       # API 엔드포인트 관리
│   ├── AuthHelper.groovy         # JWT 토큰 인증, 재인증
│   ├── MediaHelper.groovy        # 비디오 업로드, 오디오 스트리밍
│   ├── TestDataProvider.groovy   # 8개 스토리보드 데이터
│   └── MetricsCollector.groovy   # 성능 메트릭 수집
├── scenario_a_video_upload.groovy      # 시나리오 A 스크립트
├── scenario_b_recap_streaming.groovy   # 시나리오 B 스크립트
├── phase1_scenario_a_video_upload.groovy # (기존, 사용 안함)
└── resources/
    └── test-videos/              # 테스트 비디오 파일 위치

src/test/resources/load-test/
├── setup_load_test.sh           # Phase 0 자동화 스크립트
├── README.md                    # 테스트 데이터 가이드
└── sql/
    ├── 01_create_test_users.sql      # 6,000명 사용자 생성
    ├── 02_create_recap_data.sql      # 42,000개 리캡 데이터
    └── 99_cleanup_test_data.sql     # 정리 스크립트
```

---

## 📈 성능 메트릭 확인

### 실시간 모니터링
- nGrinder 웹 UI에서 TPS, 응답시간, 에러율 실시간 확인
- MetricsCollector의 실시간 요약 로그 확인

### 최종 리포트
```
예상 메트릭:
- 총 요청 수: ~50,000건 (30분간)
- 평균 TPS: ~27 (목표 달성)
- 평균 응답시간: <500ms (목표)
- 에러율: <0.1% (목표)
- P95 응답시간: <1초

API별 세부 메트릭:
- AUTH_TEST_CALLBACK: 인증 성능
- VIDEO_UPLOAD: 업로드 성능
- AUDIO_STREAMING: 스트리밍 성능
- 각 API별 호출 횟수, 응답시간, 에러율
```

---

## 🔧 문제해결 가이드

### 일반적인 문제
1. **DB 연결 실패**: setup_load_test.sh 환경변수 확인
2. **인증 실패**: TestAuthService 동작 확인
3. **비디오 파일 없음**: 더미 파일 자동 생성됨
4. **메모리 부족**: nGrinder Agent 메모리 증가

### 성능 최적화
1. **DB 최적화**: application-loadtest.properties 사용
2. **연결 풀**: HikariCP 설정 조정
3. **JVM 튜닝**: -Xmx, -Xms 설정 조정

---

## 📋 체크리스트

### 실행 전 확인사항
- [ ] PostgreSQL 실행 중
- [ ] API 서버 실행 중 (loadtest 프로파일)
- [ ] setup_load_test.sh 성공 실행
- [ ] nGrinder Controller 실행 중
- [ ] nGrinder Agent 연결 확인

### 테스트 중 모니터링
- [ ] CPU/메모리 사용률 확인
- [ ] DB 연결 수 모니터링
- [ ] 응답시간 실시간 확인
- [ ] 에러율 실시간 확인

### 완료 후 확인사항
- [ ] 최종 성능 리포트 생성
- [ ] 성능 목표 달성 여부 확인
- [ ] 99_cleanup_test_data.sql 실행 (선택)

---

## 🎊 기대 결과

### 성공 시나리오
```
✅ 86명 동시사용자로 30분간 안정적 실행
✅ 평균 응답시간 500ms 이하 달성
✅ 에러율 0.1% 이하 달성
✅ 6,000명 사용자 데이터로 현실적 테스트
✅ 스토리보드별 세션 시간 차별화 검증
✅ 비디오 업로드 + 오디오 스트리밍 동시 부하 검증
```

### 발견 가능한 이슈
- DB 연결 풀 부족
- JWT 토큰 처리 병목
- 파일 업로드 타임아웃
- S3 접속 지연
- 메모리 누수

---

## 📞 문의 및 지원

구현된 모든 코드는 계획서 요구사항을 100% 반영하여 작성되었습니다.
추가 수정이나 최적화가 필요한 경우 언제든 요청해주세요.

**Ready for Load Testing! 🚀**
