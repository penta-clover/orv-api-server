# 🏗️ 디렉토리 구조

이 문서는 `orv-api-server` 프로젝트의 주요 디렉토리와 파일 구조를 설명합니다. 이 구조를 이해하면 특정 기능과 관련된 코드를 빠르게 찾을 수 있습니다.

---

### 1. 전체 구조

```
.
├── .dev/docs/              # 👈 이 문서들을 포함한 프로젝트 문서 폴더
├── .gradle/                # Gradle 래퍼 및 빌드 관련 캐시
├── build/                  # 컴파일된 클래스 파일, 테스트 결과, 빌드 산출물
├── docker/                 # Docker 관련 설정 (로컬 개발 환경)
├── gradle/                 # Gradle 래퍼 jar 파일
├── src/                    # ⭐️ 소스 코드 루트
│   ├── main/               # - 애플리케이션 메인 소스 코드
│   └── test/               # - 테스트 코드
└── build.gradle            # 프로젝트 의존성 및 빌드 스크립트
```

---

### 2. `src/main` - 메인 소스 코드

애플리케이션의 핵심 로직이 위치하는 곳입니다.

```
src/main/
├── java/com/orv/api/       # Java 소스 코드 루트
│   ├── ApiApplication.java # - Spring Boot 시작점
│   ├── config/             # - ⚙️ Spring Bean 설정 (SecurityConfig, S3Config 등)
│   ├── domain/             # - 🧩 비즈니스 로직의 핵심. 도메인별로 패키지 분리
│   │   ├── admin/
│   │   ├── archive/
│   │   ├── auth/           #   - 인증, 인가, 회원 관리
│   │   ├── health/
│   │   ├── media/
│   │   ├── reservation/
│   │   ├── storyboard/
│   │   └── term/
│   └── global/             # - 🌍 전역적으로 사용되는 설정, 유틸리티, DTO
│       ├── bizgo/          #   - Biz-Go SMS 서비스 연동 모듈
│       └── dto/            #   - 공통 응답 DTO 등
│
└── resources/              # 설정 파일, 정적 리소스, 템플릿 등
    ├── application.properties # - Spring Boot 애플리케이션 설정
    ├── db/
    │   ├── migration/      # - 🗄️ Flyway 데이터베이스 마이그레이션 스크립트
    │   └── data/           # - 초기 데이터 또는 테스트 데이터 SQL
    └── static/             # - 정적 파일 (HTML, CSS, JS)
```

#### **주요 패키지 설명**

-   **`config`**: `SecurityConfig`, `S3Config`, `QuartzConfig` 등 애플리케이션의 동작 방식을 정의하는 Spring `@Configuration` 클래스들이 위치합니다.
-   **`domain`**: 이 프로젝트의 가장 핵심적인 부분입니다. 각 하위 패키지(예: `auth`, `archive`)는 하나의 비즈니스 도메인을 나타내며, 관련된 `Controller`, `Service`, `Repository`, `DTO` 클래스들을 포함합니다.
    -   **AI 에이전트 작업 가이드**: 새로운 기능을 추가하거나 기존 기능을 수정할 때는 대부분 이 `domain` 패키지 하위에서 작업을 시작하게 됩니다.
-   **`global`**: 특정 도메인에 속하지 않고 여러 곳에서 공통으로 사용되는 코드들이 위치합니다. 예를 들어, 모든 API 응답 형식에 사용되는 `ApiResponse` DTO나 외부 서비스(BizGo) 연동 코드가 여기에 해당합니다.
-   **`resources/db/migration`**: 데이터베이스 스키마 변경 이력이 담긴 SQL 파일들이 위치합니다. **AI 에이전트는 DB 스키마 변경 시 반드시 이 디렉토리에 새로운 마이그레이션 파일을 추가해야 합니다.**

---

### 3. `src/test` - 테스트 코드

`src/main`과 동일한 패키지 구조를 가지며, 단위 테스트(Unit Test)와 통합 테스트(Integration Test) 코드를 포함합니다.

-   **`unit`**: 특정 클래스나 메소드의 기능을 독립적으로 테스트합니다. (`@SpringBootTest`를 최소한으로 사용)
-   **`integration`**: 여러 컴포넌트(Controller, Service, Repository)를 함께 묶어 실제 애플리케이션과 유사한 환경에서 테스트합니다. (`@SpringBootTest` 사용)

**AI 에이전트 작업 가이드**: 새로운 기능을 추가했다면, 그 기능에 대한 테스트 코드를 `src/test` 아래에 반드시 추가해야 합니다. 이는 코드의 안정성을 보장하는 중요한 과정입니다.
