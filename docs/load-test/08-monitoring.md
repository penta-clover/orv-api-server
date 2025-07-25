# 8. 모니터링 및 분석

> **[← 이전: 실행 계획](07-execution-plan.md)** | **[메인으로 돌아가기](README.md)** | **[다음: 테스트 데이터 생성 가이드 →](appendix-test-data-guide.md)**

## 8.1 실시간 모니터링

### 📊 nGrinder Dashboard

#### 핵심 메트릭
- **TPS (Transaction Per Second)**
  - 초당 처리되는 트랜잭션 수
  - 목표: 100 TPS 이상 유지
  - 모니터링 포인트: 급격한 하락 시 병목 발생 의심

- **Mean Test Time**
  - 평균 응답 시간
  - 목표: 500ms 이하 (95 percentile)
  - 모니터링 포인트: 1초 초과 시 성능 저하 경고

- **Error Rate**
  - HTTP 4xx/5xx 에러 발생률
  - 목표: 0.1% 이하
  - 모니터링 포인트: 1% 초과 시 즉시 조치 필요

- **Active Users**
  - 현재 활성 가상 사용자 수
  - Phase별 목표치와 실제값 비교
  - 모니터링 포인트: 목표 VU 수 달성 여부

### ☁️ CloudWatch Dashboard

#### A. EC2 메트릭 (API 서버)
```json
{
  "widgets": [
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/EC2", "CPUUtilization", "InstanceId", "i-xxxxxxxxx"],
          ["CWAgent", "MemoryUtilization", "InstanceId", "i-xxxxxxxxx"],
          ["AWS/EC2", "NetworkIn", "InstanceId", "i-xxxxxxxxx"],
          ["AWS/EC2", "NetworkOut", "InstanceId", "i-xxxxxxxxx"]
        ],
        "period": 300,
        "stat": "Average",
        "region": "ap-northeast-2",
        "title": "API Server Metrics"
      }
    }
  ]
}
```

**주요 관찰 포인트**:
- **CPU 사용률**: 70% 이하 유지 (85% 초과 시 경고)
- **메모리 사용률**: 80% 이하 유지 (90% 초과 시 위험)
- **네트워크 I/O**: 급격한 증가 시 대역폭 병목 확인

#### B. RDS 메트릭 (PostgreSQL)
```json
{
  "widgets": [
    {
      "type": "metric", 
      "properties": {
        "metrics": [
          ["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", "orv-db"],
          ["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", "orv-db"],
          ["AWS/RDS", "ReadIOPS", "DBInstanceIdentifier", "orv-db"],
          ["AWS/RDS", "WriteIOPS", "DBInstanceIdentifier", "orv-db"]
        ],
        "title": "Database Metrics"
      }
    }
  ]
}
```

**주요 관찰 포인트**:
- **DB 연결 수**: 50개 이하 유지 (80개 초과 시 Connection Pool 확장 필요)
- **DB CPU**: 70% 이하 유지
- **IOPS**: 급격한 증가 시 쿼리 최적화 필요

#### C. S3 메트릭 (파일 스토리지)
```json
{
  "widgets": [
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/S3", "AllRequests", "BucketName", "orv-test-bucket"],
          ["AWS/S3", "BytesDownloaded", "BucketName", "orv-test-bucket"],
          ["AWS/S3", "BytesUploaded", "BucketName", "orv-test-bucket"]
        ],
        "title": "S3 Storage Metrics"
      }
    }
  ]
}
```

**주요 관찰 포인트**:
- **요청 수**: 영상 업로드/다운로드 패턴 분석
- **대역폭**: 동시 접속자 증가에 따른 처리량 변화
- **에러율**: S3 접근 실패 여부 모니터링

---

## 8.2 로그 수집

### 📝 로그 분류 및 수집 경로

#### A. Application Logs → CloudWatch Logs
```bash
# 로그 그룹: /aws/ec2/orv-api-server
# 주요 수집 대상:
- 애플리케이션 시작/종료 로그
- API 요청/응답 로그 (성능 측정용)
- 비즈니스 로직 처리 로그
- 예외 및 에러 로그
```

**로그 레벨별 분류**:
```properties
# logback-spring.xml 설정 예시  
<logger name="com.orv.api" level="INFO"/>
<logger name="com.orv.api.domain.auth" level="DEBUG"/> # 테스트 인증 디버깅
<logger name="com.orv.api.domain.reservation" level="DEBUG"/> # 리캡 처리 디버깅
<logger name="org.springframework.web" level="WARN"/>
<logger name="com.zaxxer.hikari" level="DEBUG"/> # Connection Pool 모니터링
```

#### B. Access Logs → S3
```bash
# S3 버킷: orv-access-logs/load-test/
# 수집 내용:
- HTTP 요청 메서드, URI, 응답 코드
- 사용자 IP, User-Agent
- 요청 처리 시간
- 데이터 전송량
```

**로그 포맷 예시**:
```
[2025-07-25 02:45:00] 127.0.0.1 GET /api/v0/topic/list 200 245ms 1.2KB "nGrinder/3.5.8"
[2025-07-25 02:45:01] 127.0.0.1 POST /api/v0/archive/recorded-video 201 5.2s 5.1MB "nGrinder/3.5.8"
```

#### C. Error Logs → CloudWatch Logs Insights
```sql
# 에러 패턴 분석 쿼리
fields @timestamp, @message
| filter @message like /ERROR/
| stats count(*) by bin(5m)
| sort @timestamp desc

# 응답 시간 분석 쿼리  
fields @timestamp, @message
| filter @message like /Request processed/
| parse @message "processed in * ms" as response_time
| stats avg(response_time), max(response_time), count() by bin(5m)
```

---

## 8.3 분석 메트릭

### 🎯 성능 지표 분석

#### A. Throughput (처리량) 분석
```bash
# TPS 계산 공식
TPS = 총 성공한 요청 수 / 테스트 시간(초)

# API별 TPS 분석
- Scene 조회 API: ~667 TPS (24,000회/36분)
- 스토리보드 미리보기: ~333 TPS (12,000회/36분)  
- 영상 업로드: ~83 TPS (3,000회/36분)
- 영상 다운로드: ~83 TPS (3,000회/36분)
```

#### B. Response Time Distribution (응답시간 분포)
```json
{
  "metrics": {
    "mean": "평균 응답시간",
    "median": "중간값 (50 percentile)",
    "p95": "95 percentile (목표: 500ms 이하)",
    "p99": "99 percentile (목표: 1000ms 이하)",
    "max": "최대 응답시간"
  }
}
```

**응답시간 목표별 분석**:
| API 카테고리 | 평균 | 95% | 99% | 최대 |
|-------------|------|-----|-----|------|
| **인증 API** | <100ms | <200ms | <300ms | <500ms |
| **조회 API** | <200ms | <300ms | <500ms | <1000ms |
| **생성 API** | <300ms | <500ms | <800ms | <1500ms |
| **파일 업로드** | <5s | <8s | <10s | <15s |
| **파일 다운로드** | <10s | <15s | <20s | <30s |

#### C. Error Rate by API (API별 에러율)
```sql
# CloudWatch Insights 쿼리
fields @timestamp, @message
| filter @message like /HTTP/
| parse @message "* * * *" as method, uri, status, response_time
| stats 
    count() as total_requests,
    sum(case status >= "400" when 1 else 0 end) as error_count
    by uri
| extend error_rate = error_count / total_requests * 100
| sort error_rate desc
```

#### D. Resource Utilization (리소스 사용률)
```bash
# CPU 사용률 패턴 분석
- Baseline: 15-25% (유휴 상태)
- Ramp-up: 25-50% (점진적 증가)  
- Peak: 50-70% (목표 부하)
- Spike: 70-85% (최대 부하)
- Recovery: 50-70% → 25% (복구)

# 메모리 사용률 패턴
- Heap Memory: 2-4GB 사용 (최대 8GB)
- Connection Pool: 10-45개 사용 (최대 50개)
- Thread Pool: 50-100개 사용 (최대 200개)
```

---

## 8.4 병목 분석

### 🔍 성능 병목 지점 식별

#### A. Slow Query Analysis (느린 쿼리 분석)
```sql
# PostgreSQL slow query 분석
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    stddev_time
FROM pg_stat_statements 
WHERE mean_time > 100  -- 100ms 이상 쿼리
ORDER BY mean_time DESC;
```

**예상 병목 쿼리**:
- Scene 조회 쿼리 (복잡한 JOIN)
- 사용자별 영상 목록 조회 (LIMIT/OFFSET)
- 리캡 데이터 aggregation 쿼리

#### B. API Response Time Breakdown
```json
{
  "breakdown": {
    "authentication": "JWT 토큰 검증 시간",
    "business_logic": "비즈니스 로직 처리 시간", 
    "database_query": "데이터베이스 쿼리 시간",
    "external_api": "외부 API 호출 시간",
    "response_serialization": "응답 직렬화 시간"
  }
}
```

#### C. Network Latency 분석
- **nGrinder Agent → API Server**: <10ms
- **API Server → RDS**: <5ms  
- **API Server → S3**: <20ms
- **클라이언트 → CloudFront**: <50ms

#### D. I/O Wait Time 모니터링
```bash
# 시스템 I/O 대기 시간 분석
iostat -x 1
# %iowait 지표 모니터링 (10% 이하 유지)

# 애플리케이션 I/O 패턴
- 영상 업로드: Sequential Write (높은 I/O)
- 데이터베이스: Random Read/Write (낮은-중간 I/O)
- 로그 기록: Sequential Write (낮은 I/O)
```

---

## 8.5 리포트 템플릿

### 📄 Executive Summary (경영진 요약)

```markdown
# ORV API Server 부하테스트 결과 요약

## 테스트 개요
- **테스트 일시**: 2025년 7월 25일 14:00-14:35
- **테스트 시간**: 35분 (Ramp-up 10분 + Peak 15분 + Spike 5분 + Recovery 5분)
- **최대 동시 사용자**: 258명 (목표 대비 300% 부하)
- **총 API 호출**: 약 45,000회

## 핵심 결과
✅ **성능 목표 달성**: 평균 TPS 120 (목표 100 이상)
✅ **응답시간 목표 달성**: 95% 응답시간 450ms (목표 500ms 이하)  
✅ **안정성 확인**: 에러율 0.08% (목표 0.1% 이하)
⚠️ **개선 필요**: CPU 사용률 최대 78% (목표 70% 이하 초과)

## 권장사항
1. **단기**: DB Connection Pool 75개로 확장
2. **중기**: Scene 조회 쿼리 최적화 
3. **장기**: 캐시 시스템 도입 검토
```

### 📊 Test Configuration (테스트 구성)

```yaml
test_environment:
  api_server:
    instance_type: "t3.large"
    cpu_cores: 2
    memory: "8GB"
    connection_pool: 50
    
  database:
    instance_class: "db.t3.small"
    cpu_cores: 2
    memory: "2GB" 
    max_connections: 100
    
  load_generator:
    tool: "nGrinder 3.5.8"
    agents: 4
    max_vusers: 300
    
test_scenarios:
  scenario_a:
    name: "컨텐츠 생성형"
    users: 50%
    session_duration: "8-11분"
    api_calls: "18-25회"
    
  scenario_b:
    name: "컨텐츠 소비형" 
    users: 50%
    session_duration: "5분"
    api_calls: "4회"
```

### 📈 Performance Metrics (성능 메트릭)

```json
{
  "throughput": {
    "peak_tps": 145,
    "average_tps": 120,
    "total_requests": 45236,
    "successful_requests": 45202,
    "failed_requests": 34
  },
  "response_time": {
    "mean": 285,
    "median": 220,
    "p95": 450,
    "p99": 680,
    "max": 1240
  },
  "error_analysis": {
    "total_error_rate": 0.08,
    "http_4xx": 12,
    "http_5xx": 22,
    "timeout_errors": 0,
    "connection_errors": 0
  },
  "resource_utilization": {
    "max_cpu_usage": 78,
    "max_memory_usage": 65,
    "max_db_connections": 42,
    "max_network_io": "120 Mbps"
  }
}
```

### 🔧 Bottleneck Analysis (병목 분석)

#### 식별된 성능 병목
1. **Scene 조회 API 응답시간**
   - **현상**: 평균 응답시간 320ms (목표 300ms 초과)
   - **원인**: 복잡한 JOIN 쿼리와 인덱스 부족
   - **해결방안**: Scene 테이블 인덱스 추가, 쿼리 최적화

2. **DB Connection Pool 사용률**
   - **현상**: 최대 84% 사용률 (42/50개)
   - **원인**: 장시간 세션 유지와 동시 접속 증가
   - **해결방안**: Connection Pool 크기 75개로 확장

3. **메모리 사용 패턴**
   - **현상**: GC 빈도 증가, 일시적 지연 발생
   - **원인**: 대용량 영상 처리 시 메모리 사용량 급증
   - **해결방안**: JVM 힙 크기 10GB로 확장, GC 튜닝

### 💡 Recommendations (권장사항)

#### 즉시 조치 (1주 내)
- [ ] **DB Connection Pool 확장**: 50개 → 75개
- [ ] **Scene 테이블 인덱스 추가**: (storyboard_id, scene_order)
- [ ] **JVM 메모리 설정 최적화**: -Xmx10g -Xms8g

#### 단기 개선 (1개월 내)  
- [ ] **쿼리 최적화**: Scene 조회 쿼리 리팩토링
- [ ] **Connection Pool 모니터링**: HikariCP 메트릭 대시보드 구축
- [ ] **캐시 도입 검토**: Redis 기반 Scene 데이터 캐싱

#### 장기 계획 (3개월 내)
- [ ] **아키텍처 개선**: Scene 데이터 정규화 및 캐시 계층 도입
- [ ] **오토스케일링**: 부하 증가 시 자동 인스턴스 확장
- [ ] **CDN 최적화**: 정적 리소스 캐시 전략 개선

### 📋 Appendix (Raw Data)

```bash
# 상세 메트릭 데이터 위치
- nGrinder 리포트: /reports/orv-loadtest-20250725.html
- CloudWatch 대시보드: https://console.aws.amazon.com/cloudwatch/
- 로그 아카이브: s3://orv-logs/load-test/20250725/
- 성능 프로파일: /profiles/jvm-profile-20250725.hprof
```

## 📋 관련 문서

- **이전 단계**: [실행 계획](07-execution-plan.md)에서 테스트 실행 절차 확인
- **다음 단계**: [테스트 데이터 생성 가이드](appendix-test-data-guide.md)에서 구현 상세 내용 확인
- **참조**: [성능 목표](03-performance-targets.md)에서 목표 지표와 실제 결과 비교

---

**[← 이전: 실행 계획](07-execution-plan.md)** | **[메인으로 돌아가기](README.md)** | **[다음: 테스트 데이터 생성 가이드 →](appendix-test-data-guide.md)**
