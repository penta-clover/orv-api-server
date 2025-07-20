# Recap API 명세

> **소개**: 리캡(Recap) 생성 및 조회 관련 API 명세입니다.

---

### 1. API 목록

| HTTP Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/v1/recap-reservations` | 리캡 생성 예약 |
| `GET` | `/api/v1/recap-results/{recapResultId}` | 특정 리캡 결과 조회 |

---

### 2. 상세 명세

#### `POST /api/v1/recap-reservations`

리캡 생성을 예약합니다.

-   **Request Body**:
    ```json
    {
        "videoId": "string (UUID)",
        "scheduledAt": "string (ISO 8601 format, e.g., '2025-07-07T10:00:00Z')",
        "interviewAudioRecordingId": "string (UUID)"
    }
    ```

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "리캡 생성이 예약되었습니다.",
        "data": {
            "recapReservationId": "string (UUID)"
        }
    }
    ```

#### `GET /api/v1/recap-results/{recapResultId}`

특정 리캡 결과를 조회합니다.

-   **Path Variables**:
    -   `recapResultId`: `string (UUID)` - 조회할 리캡 결과의 ID

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": {
            "id": "string (UUID)",
            "createdAt": "string (ISO 8601 format)",
            "answerSummaries": [
                {
                    "id": "string (UUID)",
                    "sceneId": "string (UUID)",
                    "summary": "string",
                    "sceneOrder": "integer"
                }
            ]
        }
    }
    ```
