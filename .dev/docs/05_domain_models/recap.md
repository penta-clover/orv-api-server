# Recap 도메인 모델

> **설명**: 리캡(Recap) 생성 및 결과와 관련된 데이터 모델과 비즈니스 로직을 설명합니다.

---

### 1. 주요 엔티티

-   **`RecapReservation`**: 리캡 생성 예약 정보를 나타냅니다.
    -   `id`: 리캡 예약 ID (UUID)
    -   `memberId`: 예약한 회원 ID (FK)
    -   `videoId`: 원본 영상 ID (FK)
    -   `scheduledAt`: 리캡 생성 예약 시간
    -   `interviewAudioRecordingId`: 리캡 생성에 사용될 오디오 레코딩 ID (FK)
    -   `recapResultId`: 생성된 리캡 결과 ID (FK)

-   **`RecapResult`**: 생성된 리캡의 최종 결과 메타데이터를 나타냅니다.
    -   `id`: 리캡 결과 ID (UUID)
    -   `createdAt`: 리캡 결과 생성 일시

-   **`RecapAnswerSummary`**: 리캡 결과에 포함된 각 답변 씬(Scene)의 요약 정보를 나타냅니다.
    -   `id`: 리캡 답변 요약 ID (UUID)
    -   `recapResultId`: 이 요약이 속한 리캡 결과 ID (FK)
    -   `sceneId`: 요약된 씬의 ID (FK)
    -   `summary`: 씬의 요약 내용
    -   `sceneOrder`: 리캡 내에서의 씬 순서

### 2. 핵심 비즈니스 로직

-   **리캡 생성 예약**: 사용자가 특정 영상과 오디오 레코딩을 기반으로 리캡 생성을 예약합니다.
-   **리캡 결과 조회**: 생성된 리캡 결과를 조회하고, 각 씬별 요약 정보를 제공합니다.
