# 📖 [중요] AI 에이전트를 위한 프로젝트 가이드

이 문서는 orv-api-server 프로젝트의 작업을 시작하기 전에 AI 코딩 에이전트가 가장 먼저 읽어야 할 핵심 가이드입니다. 이 프로젝트의 목표, 구조, 주요 규칙을 숙지하면 보다 정확하고 효율적인 작업이 가능합니다.

---

### 📍 프로젝트 한 줄 요약

**사용자 참여형 인터뷰 아카이빙 및 영상 생성 플랫폼 API 서버**

### 🎯 핵심 목표 (Mission)

- 사용자들이 주어진 주제에 대해 자신의 이야기를 영상으로 녹화하고 공유할 수 있는 플랫폼을 제공합니다.
- 녹화된 영상들을 아카이빙하고, 다양한 콘텐츠로 재가공할 수 있는 기반을 마련합니다.
- 안정적이고 확장 가능한 API를 통해 다양한 클라이언트(웹, 모바일 앱)를 지원합니다.

---

### 🛠️ 기술 스택 (Tech Stack)

| 카테고리 | 기술 | 버전 | 비고 |
|---|---|---|---|
| 언어 | Java | 17 | |
| 프레임워크 | Spring Boot | 3.x | |
| 빌드 도구 | Gradle | 8.x | |
| 데이터베이스 | PostgreSQL | 15 | |
| DB 마이그레이션 | Flyway | 9.x | |
| 클라우드 스토리지| AWS S3 | - | LocalStack으로 로컬 테스트 환경 제공 |
| 인증 | JWT (JSON Web Token) | - | |
| 스케줄링 | Quartz | - | |

---

### ⚖️ AI 에이전트를 위한 필수 준수 규칙 (Golden Rules)

> **경고: 아래 규칙을 위반하면 시스템 오류 또는 데이터 불일치를 유발할 수 있습니다. 반드시 준수하십시오.**

1.  **코드 변경 시 문서 업데이트 필수**: 코드(API, DB 스키마, 주요 로직 등)를 변경할 경우, 반드시 `.dev/docs/08_documentation_policy/01_documentation_update_rules.md` 문서를 확인하고 관련된 모든 문서를 **같은 커밋 내에서** 최신화해야 합니다.

2.  **DB 스키마 변경은 Flyway로만**: 데이터베이스 테이블 구조 변경, 컬럼 추가/삭제 등 모든 스키마 변경은 반드시 `src/main/resources/db/migration` 경로에 `V{버전}__{설명}.sql` 형식의 Flyway 마이그레이션 스크립트를 통해서만 이루어져야 합니다.

---

### 📚 가장 먼저 읽어야 할 문서 Top 3

1.  `01_architecture/01_system_architecture.md`: 시스템의 전체 구조와 설계 사상을 파악합니다.
2.  `02_getting_started/01_development_setup.md`: 로컬 개발 환경을 설정하고 서버를 실행합니다.
3.  `08_documentation_policy/01_documentation_update_rules.md`: 코드 변경 시 따라야 할 문서화 규칙을 숙지합니다.

---

### 🚀 주요 명령어

- **전체 빌드 및 테스트**:
  ```bash
  ./gradlew clean build
  ```
- **로컬 서버 실행 (Docker 컨테이너 먼저 실행 필요)**:
  ```bash
  ./gradlew bootRun
  ```
- **로컬 개발 환경(PostgreSQL, LocalStack) 실행**:
  ```bash
  docker-compose -f docker/dev/docker-compose.yaml up -d
