# 6. 인프라 구성

> **[← 이전: 구현 계획](05-implementation-plan.md)** | **[메인으로 돌아가기](README.md)** | **[다음: 실행 계획 →](07-execution-plan.md)**

## 6.1 기존 환경 활용

**AWS 인프라는 이미 구축되어 있으므로 별도 구성 작업이 불필요합니다.**

### 확인이 필요한 기존 리소스

- **API Server**: EC2 인스턴스 (운영 환경과 동일한 스펙)
- **Database**: RDS PostgreSQL 15
- **Storage**: S3 버킷
- **nGrinder**: Controller 및 Agent 서버

## 6.2 사전 환경 체크리스트

### 🔍 필수 확인 사항

#### A. API 서버 접근성 검증
- [ ] **API 서버 접근 가능 여부 확인**
  - [ ] HTTP/HTTPS 연결 테스트
  - [ ] 네트워크 레이턴시 측정
  - [ ] 방화벽 설정 확인
- [ ] **애플리케이션 상태 검증**
  - [ ] Health Check 엔드포인트 호출
  - [ ] 기본 API 응답 확인
  - [ ] 로그 수집 상태 점검

#### B. 데이터베이스 연결 검증
- [ ] **데이터베이스 연결 상태 확인**
  - [ ] PostgreSQL 15 접속 테스트
  - [ ] Connection Pool 설정 확인
  - [ ] 스키마 무결성 검증
- [ ] **성능 관련 설정 확인**
  - [ ] max_connections 설정값 확인
  - [ ] shared_buffers 최적화 상태
  - [ ] 인덱스 상태 점검

#### C. S3 스토리지 권한 검증
- [ ] **S3 버킷 읽기/쓰기 권한 확인**
  - [ ] 영상 업로드 테스트
  - [ ] 파일 다운로드 테스트
  - [ ] CORS 설정 확인
- [ ] **CloudFront 배포 상태**
  - [ ] CDN 캐시 설정 확인
  - [ ] 오리진 접근 정책 검증
  - [ ] 압축 설정 최적화

#### D. nGrinder 서버 상태 점검
- [ ] **nGrinder Controller/Agent 상태 확인**
  - [ ] Controller 웹 인터페이스 접속
  - [ ] Agent 연결 상태 확인
  - [ ] 리소스 사용량 모니터링
- [ ] **테스트 실행 환경 준비**
  - [ ] 스크립트 업로드 경로 확인
  - [ ] 결과 저장 공간 확보
  - [ ] 백업 및 복구 계획 수립

#### E. 네트워크 대역폭 및 보안그룹 설정
- [ ] **네트워크 대역폭 확인**
  - [ ] 인바운드/아웃바운드 대역폭 측정
  - [ ] 동시 연결 수 제한 확인
  - [ ] 네트워크 지연시간 측정
- [ ] **보안그룹 설정 점검**
  - [ ] 필요한 포트 개방 확인
  - [ ] IP 화이트리스트 설정
  - [ ] SSL/TLS 인증서 유효성 검증

---

## 6.3 환경별 설정 가이드

### 개발 환경 (Development)
```yaml
# 개발환경 설정 예시
api_server:
  instances: 1
  instance_type: t3.medium
  cpu_limit: 70%
  memory_limit: 4GB

database:
  instance_class: db.t3.micro
  max_connections: 20
  connection_timeout: 30s

ngrinder:
  controller: 1 instance
  agents: 2 instances
  max_vusers: 50
```

### 부하테스트 환경 (Load Test)
```yaml
# 부하테스트 환경 설정
api_server:
  instances: 1-2 (오토스케일링)
  instance_type: t3.large
  cpu_limit: 85%
  memory_limit: 8GB

database:
  instance_class: db.t3.small
  max_connections: 100
  connection_timeout: 10s

ngrinder:
  controller: 1 instance (t3.medium)
  agents: 3-5 instances (t3.small)
  max_vusers: 300+
```

---

## 6.4 모니터링 인프라 설정

### CloudWatch 대시보드 구성

#### A. API 서버 메트릭
```json
{
  "metrics": [
    {
      "name": "CPUUtilization",
      "namespace": "AWS/EC2",
      "dimension": "InstanceId"
    },
    {
      "name": "MemoryUtilization", 
      "namespace": "CWAgent",
      "dimension": "InstanceId"
    },
    {
      "name": "NetworkIn/NetworkOut",
      "namespace": "AWS/EC2",
      "dimension": "InstanceId"
    }
  ],
  "period": 300,
  "stat": "Average"
}
```

#### B. RDS 메트릭
```json
{
  "metrics": [
    {
      "name": "DatabaseConnections",
      "namespace": "AWS/RDS",
      "dimension": "DBInstanceIdentifier"
    },
    {
      "name": "CPUUtilization",
      "namespace": "AWS/RDS", 
      "dimension": "DBInstanceIdentifier"
    },
    {
      "name": "ReadIOPS/WriteIOPS",
      "namespace": "AWS/RDS",
      "dimension": "DBInstanceIdentifier"
    }
  ]
}
```

#### C. S3 메트릭
```json
{
  "metrics": [
    {
      "name": "NumberOfObjects",
      "namespace": "AWS/S3",
      "dimension": "BucketName"
    },
    {
      "name": "AllRequests",
      "namespace": "AWS/S3",
      "dimension": "BucketName"
    },
    {
      "name": "BytesDownloaded/BytesUploaded",
      "namespace": "AWS/S3",
      "dimension": "BucketName"
    }
  ]
}
```

### 알람 설정

| 메트릭 | 임계값 | 알람 조건 | 액션 |
|--------|--------|-----------|------|
| **API 서버 CPU** | > 80% | 3분간 지속 | SNS 알림 |
| **DB 연결 수** | > 80 | 2분간 지속 | 즉시 알림 |
| **메모리 사용률** | > 90% | 5분간 지속 | SNS 알림 |
| **네트워크 에러율** | > 1% | 1분간 지속 | 즉시 알림 |

---

## 6.5 보안 및 접근 제어

### IAM 역할 및 정책

#### A. nGrinder 실행을 위한 IAM 역할
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "cloudwatch:GetMetricStatistics",
        "cloudwatch:PutMetricData",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow", 
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::test-bucket/*"
    }
  ]
}
```

#### B. 부하테스트 전용 보안 그룹
```yaml
# 인바운드 규칙
rules:
  - port: 80
    protocol: HTTP
    source: ngrinder-agents
  - port: 443  
    protocol: HTTPS
    source: ngrinder-agents
  - port: 5432
    protocol: PostgreSQL
    source: api-servers

# 아웃바운드 규칙  
egress:
  - port: 80/443
    protocol: HTTP/HTTPS
    destination: 0.0.0.0/0
  - port: 5432
    protocol: PostgreSQL  
    destination: rds-subnet
```

---

## 6.6 비용 최적화

### 리소스 사용량 추정

| 리소스 | 사용 시간 | 예상 비용 (시간당) | 총 예상 비용 |
|--------|-----------|-------------------|-------------|
| **EC2 (API 서버)** | 2시간 | $0.10 | $0.20 |
| **RDS (PostgreSQL)** | 2시간 | $0.05 | $0.10 |
| **nGrinder Agents** | 2시간 | $0.15 | $0.30 |
| **S3 Storage** | 1GB | $0.02/월 | $0.02 |
| **CloudWatch** | 메트릭 수집 | $0.01 | $0.01 |
| **총계** | - | - | **~$0.63** |

### 비용 절감 방안

- [ ] **스팟 인스턴스 활용**
  - [ ] nGrinder Agent를 스팟 인스턴스로 구성
  - [ ] 비용 70% 절감 가능
- [ ] **테스트 완료 후 자동 종료**
  - [ ] Lambda 함수로 자동 정리 스크립트 실행
  - [ ] 불필요한 리소스 즉시 제거
- [ ] **CloudWatch Logs 보존 기간 설정**
  - [ ] 7일 보존으로 설정
  - [ ] 장기 보관이 필요한 경우 S3로 아카이브

---

## 6.7 장애 대응 계획

### 예상 장애 시나리오 및 대응

| 장애 유형 | 예상 원인 | 대응 방안 | 복구 시간 |
|----------|-----------|-----------|----------|
| **API 서버 다운** | 높은 부하, 메모리 부족 | 인스턴스 재시작, 스케일 아웃 | 5분 |
| **DB 연결 실패** | Connection Pool 고갈 | 연결 수 증가, 애플리케이션 재시작 | 3분 |
| **nGrinder Agent 실패** | 네트워크 문제 | Agent 재시작, 다른 AZ로 이동 | 2분 |
| **S3 접근 불가** | 권한 문제, 네트워크 | IAM 정책 확인, VPC 엔드포인트 점검 | 10분 |

### 긴급 연락망

| 역할 | 담당자 | 연락처 | 대응 범위 |
|------|--------|--------|----------|
| **부하테스트 담당** | [담당자명] | [연락처] | 테스트 스크립트, 시나리오 |
| **인프라 지원** | [인프라팀] | [연락처] | AWS 리소스, 네트워크 |
| **애플리케이션 개발** | [개발팀] | [연락처] | API 서버, 데이터베이스 |
| **긴급 연락처** | [관리자] | [연락처] | 전체 조율, 의사결정 |

---

## 6.8 환경 체크리스트 템플릿

### 테스트 실행 전 최종 점검

```bash
#!/bin/bash
# pre-test-check.sh

echo "=== 부하테스트 환경 점검 시작 ==="

# API 서버 상태 확인
echo "1. API 서버 Health Check..."
curl -f http://api-server/health || echo "❌ API 서버 응답 없음"

# 데이터베이스 연결 확인  
echo "2. 데이터베이스 연결 확인..."
psql -h db-host -U user -d dbname -c "SELECT 1;" || echo "❌ DB 연결 실패"

# S3 접근 권한 확인
echo "3. S3 접근 권한 확인..."
aws s3 ls s3://test-bucket/ || echo "❌ S3 접근 불가"

# nGrinder 상태 확인
echo "4. nGrinder Controller 확인..."
curl -f http://ngrinder-controller:8080 || echo "❌ nGrinder 접근 불가"

echo "=== 환경 점검 완료 ==="
```

### 테스트 완료 후 정리 스크립트

```bash
#!/bin/bash
# post-test-cleanup.sh

echo "=== 부하테스트 후 정리 작업 시작 ==="

# 테스트 데이터 정리
echo "1. 테스트 데이터 정리 중..."
psql -h db-host -U user -d dbname -f cleanup-test-data.sql

# S3 임시 파일 정리
echo "2. S3 임시 파일 정리 중..."
aws s3 rm s3://test-bucket/temp/ --recursive

# CloudWatch 로그 압축
echo "3. 로그 압축 및 아카이브..."
aws logs create-export-task --log-group-name /aws/ec2/api-server

echo "=== 정리 작업 완료 ==="
```

## 📋 관련 문서

- **이전 단계**: [구현 계획](05-implementation-plan.md)에서 Phase별 구현 일정 확인
- **다음 단계**: [실행 계획](07-execution-plan.md)에서 35분 테스트 시나리오 확인  
- **모니터링**: [모니터링 및 분석](08-monitoring.md)에서 실시간 모니터링 방법 확인

---

**[← 이전: 구현 계획](05-implementation-plan.md)** | **[메인으로 돌아가기](README.md)** | **[다음: 실행 계획 →](07-execution-plan.md)**
