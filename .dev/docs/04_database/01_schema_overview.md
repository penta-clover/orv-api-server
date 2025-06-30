# 🗄️ 데이터베이스 스키마 개요

이 문서는 `orv-api-server`의 주요 데이터베이스 테이블과 그 관계를 설명합니다. 모든 스키마는 `src/main/resources/db/migration`에 위치한 Flyway 스크립트를 통해 관리됩니다.

---

### 1. 주요 테이블 ERD (Entity-Relationship Diagram)

아래 다이어그램은 핵심 도메인과 관련된 주요 테이블 간의 관계를 보여줍니다.

```mermaid
erDiagram
    MEMBER {
        uuid id PK "회원 ID"
        varchar name "이름"
        varchar email "이메일"
        varchar profile_image_url "프로필 이미지 URL"
        varchar provider "소셜 제공자 (google, kakao)"
        varchar provider_id "소셜 ID"
        timestamptz created_at
    }

    TOPIC {
        bigint id PK "주제 ID"
        varchar title "주제"
        varchar description "설명"
        varchar thumbnail_url "썸네일 URL"
    }

    STORYBOARD {
        uuid id PK "스토리보드 ID"
        bigint member_id FK "회원 ID"
        bigint topic_id FK "주제 ID"
        varchar title "제목"
        timestamptz created_at
    }

    SCENE {
        uuid id PK "씬(장면) ID"
        uuid storyboard_id FK "스토리보드 ID"
        int "order" "순서"
        varchar script "대본"
        varchar video_url "영상 URL"
    }

    RESERVATION {
        uuid id PK "예약 ID"
        bigint member_id FK "회원 ID"
        bigint topic_id FK "주제 ID"
        timestamptz reserved_at "예약 시간"
        varchar state "예약 상태 (예: PENDING, CONFIRMED, CANCELED, COMPLETED)"
    }

    MEMBER ||--|{ STORYBOARD : "creates"
    TOPIC ||--|{ STORYBOARD : "is_about"
    STORYBOARD ||--o{ SCENE : "contains"
    MEMBER ||--|{ RESERVATION : "makes"
    TOPIC ||--|{ RESERVATION : "is_for"
    MEMBER ||--|{ INTERVIEW_AUDIO_RECORDING : "owns"
    STORYBOARD ||--|{ INTERVIEW_AUDIO_RECORDING : "contains"
```

---

### 2. 주요 테이블 설명

#### **`INTERVIEW_AUDIO_RECORDING`**
-   **설명**: 인터뷰에서 추출된 오디오 파일의 메타데이터를 저장합니다.
-   **주요 컬럼**:
    -   `id`: 기본 키 (UUID).
    -   `storyboard_id`: 이 오디오가 속한 스토리보드 (FK).
    -   `member_id`: uuid (FK) "이 오디오의 소유자 회원".
    -   `video_url`: S3에 저장된 오디오 파일의 URL.
    -   `created_at`: 오디오 레코딩 생성 일시.
    -   `running_time`: 오디오의 재생 시간 (초).

#### **`MEMBER`**
-   **설명**: 서비스에 가입한 사용자 정보를 저장합니다. 소셜 로그인을 통해 가입하며, `provider`와 `provider_id`로 각 소셜 계정을 식별합니다.
-   **주요 컬럼**:
    -   `id`: 기본 키 (PK).
    -   `email`: 사용자 식별을 위한 이메일.
    -   `provider`, `provider_id`: 어떤 소셜 플랫폼의 어떤 사용자인지를 나타내는 복합 식별자.

#### **`TOPIC`**
-   **설명**: 사용자들이 인터뷰를 진행할 수 있는 주제 목록을 저장합니다.
-   **주요 컬럼**:
    -   `id`: 기본 키.
    -   `title`: 인터뷰 주제.

#### **`STORYBOARD`**
-   **설명**: 한 명의 사용자가 특정 주제에 대해 만든 하나의 인터뷰 결과물입니다. 여러 개의 `SCENE`으로 구성됩니다.
-   **주요 컬럼**:
    -   `id`: 기본 키 (UUID).
    -   `member_id`: 이 스토리보드를 생성한 회원 (FK).
    -   `topic_id`: 이 스토리보드가 속한 주제 (FK).

#### **`SCENE`**
-   **설명**: 스토리보드를 구성하는 개별 장면(질문과 답변 영상)입니다.
-   **주요 컬럼**:
    -   `id`: 기본 키 (UUID).
    -   `storyboard_id`: 이 씬이 속한 스토리보드 (FK).
    -   `order`: 스토리보드 내에서의 장면 순서.
    -   `video_url`: 사용자가 답변한 영상이 S3에 저장된 경로.

#### **`RESERVATION`**
-   **설명**: 사용자가 특정 주제에 대해 인터뷰를 진행하기 위해 예약한 정보를 저장합니다.
-   **주요 컬럼**:
    -   `id`: 기본 키 (UUID).
    -   `member_id`: 예약을 한 회원 (FK).
    -   `reserved_at`: 예약된 시간.
    -   `state`: 예약의 현재 상태 (`PENDING`, `CONFIRMED`, `CANCELED`, `COMPLETED` 등).
