# 🚀 ORV API Server 부하테스트 시스템 구축 계획서

> 이 문서는 기존의 LOAD_TEST_PLAN.md를 체계적으로 분할하여 관리하기 위한 메인 인덱스입니다.

## 📖 문서 네비게이션

### 📋 계획 단계
- **[1. 개요](01-overview.md)** - 테스트 목표와 도구 선정
- **[2. 부하테스트 시나리오](02-test-scenarios.md)** - 사용자 시나리오 A/B 상세 플로우
- **[3. 부하 분석 및 목표 설정](03-performance-targets.md)** - 성능 목표 지표와 부하 분석

### 🛠 구현 단계  
- **[4. 프로젝트 구조](04-project-structure.md)** - nGrinder 프로젝트 구성과 검증 결과
- **[5. 구현 계획](05-implementation-plan.md)** - 5단계 Phase별 구현 로드맵
- **[6. 인프라 구성](06-infrastructure.md)** - AWS 환경 설정 및 체크리스트

### 🚀 실행 단계
- **[7. 실행 계획](07-execution-plan.md)** - 35분 부하테스트 시나리오 실행
- **[8. 모니터링 및 분석](08-monitoring.md)** - 실시간 모니터링과 결과 분석

### 📚 부록
- **[테스트 데이터 생성 가이드](appendix-test-data-guide.md)** - DB 스키마 기반 구현 가이드

## 🎯 빠른 시작 가이드

### 처음 시작하는 경우
1. [개요](01-overview.md) → [시나리오](02-test-scenarios.md) → [성능 목표](03-performance-targets.md) 순서로 읽기
2. [프로젝트 구조](04-project-structure.md)에서 검증 결과 확인
3. [구현 계획](05-implementation-plan.md)의 Phase 0부터 단계별 진행

### 구현 중인 경우
- **API 구현**: [테스트 데이터 가이드](appendix-test-data-guide.md) 참조
- **인프라 설정**: [인프라 구성](06-infrastructure.md) 체크리스트 활용
- **테스트 실행**: [실행 계획](07-execution-plan.md) 절차 따르기

### 분석 및 개선
- **결과 분석**: [모니터링](08-monitoring.md)의 메트릭 가이드 활용
- **성능 개선**: [성능 목표](03-performance-targets.md)와 실제 결과 비교

## 📋 문서 요약표

| 문서 | 주요 내용 | 예상 읽기 시간 | 구현 시간 |
|------|-----------|---------------|-----------| 
| [개요](01-overview.md) | 테스트 목표, 도구 선정 | 5분 | - |
| [시나리오](02-test-scenarios.md) | 사용자 플로우, API 호출 패턴 | 15분 | - |
| [성능 목표](03-performance-targets.md) | KPI, 부하 분석 | 10분 | - |
| [프로젝트 구조](04-project-structure.md) | nGrinder 구성, 검증 결과 | 20분 | - |
| [구현 계획](05-implementation-plan.md) | 5단계 Phase별 로드맵 | 15분 | - |
| [인프라 구성](06-infrastructure.md) | AWS 환경, 체크리스트 | 10분 | 2시간 |
| [실행 계획](07-execution-plan.md) | 35분 테스트 절차 | 10분 | - |
| [모니터링](08-monitoring.md) | 실시간 분석, 리포트 | 15분 | 1시간 |
| [데이터 가이드](appendix-test-data-guide.md) | DB 스키마, 구현 상세 | 30분 | 6시간 |

## 📊 프로젝트 상태

- **계획 수립**: ✅ 완료
- **필수 보완사항**: ⚠️ 진행 중 (Phase 0)
- **구현**: ⏳ 대기 중
- **테스트 실행**: ⏳ 대기 중

## 🔗 원본 문서 정보

이 문서들은 `LOAD_TEST_PLAN.md`를 체계적으로 분할한 것입니다.
- **원본 백업**: `LOAD_TEST_PLAN_BACKUP.md` 
- **분할 일시**: 2025년 7월 24일
- **총 문서 수**: 9개 (메인 + 8개 섹션 + 1개 부록)

## 📞 연락처 및 지원

- **부하테스트 담당**: [담당자명]
- **인프라 지원**: [인프라팀]
- **긴급 연락처**: [연락처]

---

*최종 수정일: 2025년 7월 24일*  
*문서 버전: v1.0*
