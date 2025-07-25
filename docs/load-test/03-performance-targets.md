# 3. 부하 분석 및 목표 설정

> **[← 이전: 부하테스트 시나리오](02-test-scenarios.md)** | **[메인으로 돌아가기](README.md)** | **[다음: 프로젝트 구조 →](04-project-structure.md)**

## 3.1 사용자 부하 분석

### 기본 메트릭

- **MAU (Monthly Active Users)**: 6,000명 (서비스 분기 목표)
- **사용 패턴**: 각 유저는 일주일 중 4일에 걸쳐 시나리오 A, B를 각 2회씩 수행
- **피크 시간대**: 매일 19:00~24:00 (UTC+9)

### 계산된 메트릭

```
DAU_avg = MAU × (4/7) ≈ 3,429명
HAU_avg = DAU / 5 ≈ 686명
SessionDuration_avg = 10분 × 0.5 + 5분 × 0.5 = 7.5분
CAU_avg = (HAU_avg × SessionDuration_avg) / 60분 ≈ 86명
```

### 부하 분석 상세

| 메트릭 | 평균값 | 피크값 (3배) | 설명 |
|--------|--------|-------------|------| 
| **동시 접속자 (CAU)** | 86명 | 258명 | 피크시간 기준 |
| **시간당 세션** | 686개 | 2,058개 | 새로운 세션 시작 |
| **초당 API 호출** | ~100 TPS | ~300 TPS | 모든 API 합산 |
| **파일 업로드** | 43개/시간 | 129개/시간 | 7분 480p 영상 |
| **파일 다운로드** | 43개/시간 | 129개/시간 | 영상 + 오디오 |

## 3.2 성능 목표

### 🎯 핵심 성능 지표

| 지표 | 목표값 | 임계값 | 측정 방법 |
|------|--------|--------|----------| 
| **Throughput** | 최소 100 TPS | 200 TPS | nGrinder Dashboard |
| **Response Time (95th)** | 500ms 이하 | 1000ms | API별 측정 |
| **Response Time (99th)** | 1000ms 이하 | 2000ms | API별 측정 |
| **Error Rate** | 0.1% 이하 | 1% | HTTP 4xx/5xx |
| **CPU Utilization** | 70% 이하 | 85% | CloudWatch |
| **Memory Usage** | 80% 이하 | 90% | CloudWatch |
| **DB Connection Pool** | 80% 이하 | 95% | HikariCP 메트릭 |

### 📊 API별 성능 목표

| API 카테고리 | 목표 응답시간 | 예상 부하 | 중요도 |
|-------------|------------|----------|-------| 
| **인증 관련** | < 200ms | 높음 | HIGH |
| **조회 API** | < 300ms | 매우 높음 | HIGH |
| **생성/수정 API** | < 500ms | 중간 | MEDIUM |
| **파일 업로드** | < 10초 | 낮음 | HIGH |
| **파일 다운로드** | < 20초 | 낮음 | MEDIUM |

### 🔍 병목 지점 예측

1. **Scene 조회 API**: 시간당 24,000회 호출 (가장 높은 부하)
2. **DB Connection Pool**: 86명 동시 접속 vs 기본값 10개
3. **파일 I/O**: 영상 업로드/다운로드 시 네트워크 대역폭
4. **S3 성능**: 오디오 스트리밍 시 동시 요청 처리

## 3.3 부하 증가 패턴

### 테스트 실행 시나리오

```
총 실행 시간: 35분

Phase 1 - Ramp-up (10분):
├── 0분: 0 VU
├── 5분: 43 VU (50% 도달)
└── 10분: 86 VU (100% 도달)

Phase 2 - Peak Load (10분):
└── 86 VU 안정성 테스트

Phase 3 - Spike Test (5분):
└── 86 VU → 258 VU (3배 부하)

Phase 4 - Recovery (10분):
└── 258 VU → 86 VU (복구 테스트)
```

### 각 Phase별 검증 목표

| Phase | 목표 | 성공 기준 | 실패 시 조치 |
|-------|------|-----------|-------------| 
| **Ramp-up** | 점진적 부하 증가 | 에러율 < 0.1% | 증가 속도 조정 |
| **Peak Load** | 안정성 검증 | 모든 목표 지표 달성 | 용량 계획 재검토 |
| **Spike Test** | 급격한 부하 대응 | 에러율 < 1% | 오토스케일링 검토 |
| **Recovery** | 복구 능력 검증 | 정상 상태 복귀 | 알람 시스템 점검 |

## 3.4 모니터링 계획

### 실시간 관찰 지표

**nGrinder 메트릭**:
- TPS (Transaction Per Second)
- Mean Test Time
- Error Rate
- Active Virtual Users

**AWS CloudWatch 메트릭**:
- EC2: CPU, Memory, Network I/O
- RDS: DB Connections, CPU, IOPS
- S3: Request Rate, Data Transfer

**애플리케이션 메트릭**:
- HikariCP Connection Pool 사용률
- JVM Heap Memory 사용률
- GC 수행 횟수 및 시간

### 알람 설정

| 지표 | Warning | Critical | 액션 |
|------|---------|----------|------| 
| API 응답시간 | > 500ms | > 1000ms | 성능 분석 시작 |
| 에러율 | > 0.5% | > 1% | 테스트 중단 검토 |
| CPU 사용률 | > 70% | > 85% | 스케일링 검토 |
| DB 커넥션 | > 40개 | > 45개 | 커넥션 풀 확장 |

## 📋 관련 문서

- **이전 단계**: [부하테스트 시나리오](02-test-scenarios.md)에서 API 호출 패턴 확인
- **다음 단계**: [프로젝트 구조](04-project-structure.md)에서 구현 방법 확인
- **실행 참조**: [실행 계획](07-execution-plan.md)에서 실제 테스트 절차 확인
- **분석 참조**: [모니터링 및 분석](08-monitoring.md)에서 결과 분석 방법 확인

---

**[← 이전: 부하테스트 시나리오](02-test-scenarios.md)** | **[메인으로 돌아가기](README.md)** | **[다음: 프로젝트 구조 →](04-project-structure.md)**
