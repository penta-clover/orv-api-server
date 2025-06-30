# 🏗️ 시스템 아키텍처

이 문서는 `orv-api-server`의 전체 시스템 아키텍처, 구성 요소별 책임, 그리고 주요 기술 선택의 배경을 설명합니다.

---

### 1. 시스템 아키텍처 다이어그램 (C4 Model: Container Diagram)

아래 다이어그램은 우리 시스템의 주요 구성 요소(컨테이너)와 그들 간의 상호작용을 보여줍니다.

```mermaid
graph TD
    subgraph "사용자 환경"
        A[사용자 (Web/Mobile Client)]
    end

    subgraph "ORV API 시스템"
        B[API 서버 (Spring Boot Application)]
    end

    subgraph "외부 시스템 / 데이터 스토어"
        C[PostgreSQL 데이터베이스]
        D[AWS S3 (Object Storage)]
    end

    A -- "HTTPS (API 요청)" --> B
    B -- "JDBC (데이터 조회/저장)" --> C
    B -- "AWS SDK (파일 업로드/다운로드)" --> D

    style B fill:#f9f,stroke:#333,stroke-width:2px
```

-   **사용자 (Client)**: 웹 브라우저나 모바일 애플리케이션을 통해 시스템과 상호작용합니다. 모든 통신은 HTTPS를 통해 API 서버로 전달됩니다.
-   **API 서버**: Java Spring Boot로 구현된 핵심 애플리케이션입니다. 비즈니스 로직 처리, 데이터 영속성 관리, 외부 서비스 연동 등 모든 주요 기능을 담당합니다.
-   **PostgreSQL 데이터베이스**: 사용자 정보, 인터뷰 메타데이터, 예약 정보 등 정형 데이터를 저장하는 주 데이터베이스입니다.
-   **AWS S3**: 사용자가 업로드한 영상, 음성 파일 등 대용량 미디어 파일을 저장하는 오브젝트 스토리지입니다.

---

### 2. 레이어별 책임 (Layered Architecture)

API 서버는 명확한 책임 분리를 위해 전통적인 3-Layer 아키텍처를 따릅니다.

-   **`Controller` (Presentation Layer)**
    -   **책임**: HTTP 요청을 수신하고 응답을 반환합니다.
    -   **주요 역할**:
        -   요청 경로(Endpoint) 매핑 (`@RestController`, `@RequestMapping`).
        -   요청 파라미터 및 본문(Request Body) 검증 (`@Valid`).
        -   인증/인가 확인 (Spring Security를 통해 처리).
        -   Service 계층으로 작업 위임 및 결과 반환.
    -   **위치**: `com.orv.api.domain.{도메인명}` (예: `ArchiveController.java`)

-   **`Service` (Business Logic Layer)**
    -   **책임**: 핵심 비즈니스 로직을 처리합니다.
    -   **주요 역할**:
        -   트랜잭션 관리 (`@Transactional`).
        -   여러 Repository 또는 다른 Service를 조합하여 복잡한 비즈니스 요구사항 구현.
        -   DTO(Data Transfer Object)를 사용하여 계층 간 데이터 전달.
    -   **위치**: `com.orv.api.domain.{도메인명}` (예: `MemberService.java`)

-   **`Repository` (Data Access Layer)**
    -   **책임**: 데이터베이스와의 상호작용을 담당합니다.
    -   **주요 역할**:
        -   데이터 생성(Create), 조회(Read), 수정(Update), 삭제(Delete).
        -   Spring Data JPA 또는 JDBC Template/MyBatis 등을 사용하여 데이터 영속성 처리.
    -   **위치**: `com.orv.api.domain.{도메인명}` (예: `JdbcMemberRepository.java`)

---

### 3. 주요 기술 선택 이유 (Technology Rationale)

| 기술 | 선택 이유 |
|---|---|
| **Spring Boot** | 강력한 생태계, DI(의존성 주입), AOP(관점 지향 프로그래밍) 등 엔터프라이즈급 애플리케이션 개발에 필요한 기능을 표준화된 방식으로 제공하여 생산성과 안정성을 높입니다. |
| **Gradle** | Groovy/Kotlin 스크립트를 사용한 유연하고 강력한 빌드 구성이 가능하며, 멀티 모듈 프로젝트 관리에 용이합니다. |
| **PostgreSQL** | 오픈소스이면서도 강력한 기능(JSONB, Full-text search 등)과 안정성을 제공하며, 클라우드 환경에서 널리 지원됩니다. |
| **Flyway** | SQL 기반의 마이그레이션 스크립트를 통해 데이터베이스 스키마 변경 이력을 명확하게 버전 관리할 수 있어, 팀 협업 및 배포 자동화에 필수적입니다. |
| **AWS S3** | 높은 내구성과 가용성을 보장하는 대용량 파일 스토리지로, 미디어 파일을 안정적으로 저장하고 서빙하는 데 가장 적합한 솔루션입니다. |
| **JWT** | Stateless한 인증 방식을 구현하여 서버의 확장성을 높이고, 모바일 등 다양한 클라이언트 환경을 효과적으로 지원합니다. |
