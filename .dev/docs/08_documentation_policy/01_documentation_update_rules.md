# 🚨 문서 최신화 규칙 (Documentation Update Rules)

**이 문서는 `orv-api-server` 프로젝트의 일관성과 유지보수성을 위한 가장 중요한 규칙을 담고 있습니다.** AI 코딩 에이전트는 코드 변경 작업을 수행할 때, 아래의 규칙에 따라 **반드시** 관련 문서를 최신 상태로 업데이트해야 합니다.

> **경고: 문서 최신화는 선택이 아닌 필수입니다. 코드와 문서의 불일치는 심각한 혼란과 버그를 유발할 수 있습니다.**

---

### 문서 업데이트 기본 원칙

-   **동시성**: 코드 변경과 문서 업데이트는 **하나의 커밋(또는 하나의 Pull Request)**에 함께 포함되어야 합니다.
-   **정확성**: 변경된 코드의 내용이 문서에 정확하게 반영되어야 합니다.
-   **영향도 분석**: 자신의 코드 변경이 어떤 문서에 영향을 미치는지 스스로 판단하고 업데이트해야 합니다. 아래의 가이드라인을 참고하십시오.

---

### 코드 변경 유형별 필수 업데이트 문서

| 코드 변경 유형 | 영향받는 소스 코드 (예시) | **필수 업데이트 대상 문서** |
|---|---|---|
| **1. API 엔드포인트 변경** | `*Controller.java` | `/.dev/docs/03_api_specifications/{domain}.md` |
| **2. 데이터베이스 스키마 변경** | `db/migration/V*__*.sql` | `/.dev/docs/04_database/01_schema_overview.md` (ERD 및 테이블 설명) |
| **3. 주요 도메인 로직 변경**| `*Service.java` | `/.dev/docs/05_domain_models/{domain}.md` (로직 흐름, 다이어그램) |
| **4. 핵심 아키텍처/기술 변경**| `build.gradle`, `*Config.java`| `/.dev/docs/01_architecture/01_system_architecture.md` |
| **5. 인증/인가 흐름 변경** | `SecurityConfig.java`, `Jwt*` | `/.dev/docs/06_core_concepts/01_authentication_flow.md` |
| **6. 공통 DTO 또는 전역 유틸 변경** | `global/dto/*`, `global/util/*`| 관련 기능을 사용하는 모든 도메인 문서 및 API 명세 |

---

### 상세 시나리오별 가이드

#### **시나리오 1: 새로운 API 추가**
-   **상황**: `reservation` 도메인에 '예약 목록 조회' API를 추가했습니다.
-   **절차**:
    1.  `reservation` 도메인의 API 명세 파일을 엽니다: `/.dev/docs/03_api_specifications/reservation.md`
    2.  새로운 엔드포인트(`GET /api/v1/reservations`), 요청 파라미터, 성공/실패 응답 예시를 상세히 추가합니다.

#### **시나리오 2: 테이블 컬럼 추가**
-   **상황**: `MEMBER` 테이블에 `phone_number` 컬럼을 추가하는 Flyway 스크립트를 작성했습니다.
-   **절차**:
    1.  DB 스키마 개요 파일을 엽니다: `/.dev/docs/04_database/01_schema_overview.md`
    2.  Mermaid로 작성된 ERD의 `MEMBER` 엔티티에 `varchar phone_number`를 추가합니다.
    3.  `MEMBER` 테이블 설명 부분에 `phone_number` 컬럼에 대한 설명을 추가합니다.
    4.  이 컬럼이 API 응답에 포함된다면, 관련 API 명세(`auth.md`)의 응답 예시 JSON에도 `phoneNumber` 필드를 추가합니다.

#### **시나리오 3: 비즈니스 로직 변경**
-   **상황**: `auth` 도메인의 `MemberService`에서 회원 탈퇴 로직을 '즉시 삭제'에서 '비활성화 후 30일 뒤 삭제'로 변경했습니다.
-   **절차**:
    1.  `auth` 도메인 상세 파일을 엽니다: `/.dev/docs/05_domain_models/auth.md`
    2.  '주요 비즈니스 로직' 섹션의 회원 탈퇴 관련 설명을 수정합니다.
    3.  필요하다면, 변경된 로직을 설명하는 시퀀스 다이어그램을 업데이트합니다.
