# 📋 API 명세: 예약 (Reservation)

이 문서는 인터뷰 예약 및 리캡(Recap) 예약 관련 API의 상세 명세를 정의합니다.

---

## 1. 인터뷰 예약 관련 API

### 1.1 인터뷰 예약 생성

#### `POST /api/v0/reservation/interview`

특정 스토리보드에 대한 인터뷰를 예약하고, 선택적으로 즉시 시작할 수 있습니다.

-   **Query Parameters**:
    -   `startNow`: `boolean` (optional, default: `false`) - 즉시 인터뷰 시작 여부

-   **Request Body**:
    ```json
    {
        "storyboardId": "string (UUID)",
        "reservedAt": "string (ISO 8601 datetime with timezone, optional)"
    }
    ```

-   **Success Response (201 Created)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": {
            "id": "string (UUID)",
            "memberId": "string (UUID)",
            "storyboardId": "string (UUID)",
            "scheduledAt": "string (ISO 8601 datetime)",
            "createdAt": "string (ISO 8601 datetime)"
        }
    }
    ```

-   **Error Response (500 Internal Server Error)**:
    ```json
    {
        "success": false,
        "message": "예약 생성 중 오류가 발생했습니다.",
        "error": {
            "code": "UNKNOWN",
            "message": "Internal server error occurred"
        }
    }
    ```

### 1.2 인터뷰 예약 조회

#### `GET /api/v0/reservation/interview/{reservationId}`

특정 예약 ID로 인터뷰 예약 정보를 조회합니다.

-   **Path Variables**:
    -   `reservationId`: `string (UUID)` - 조회할 예약 ID

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": {
            "id": "string (UUID)",
            "memberId": "string (UUID)",
            "storyboardId": "string (UUID)",
            "scheduledAt": "string (ISO 8601 datetime)",
            "createdAt": "string (ISO 8601 datetime)"
        }
    }
    ```

-   **Not Found Response (404 Not Found)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": null
    }
    ```

### 1.3 향후 인터뷰 목록 조회

#### `GET /api/v0/reservation/interview/forward`

현재 로그인된 사용자의 향후 예정된 인터뷰 목록을 조회합니다.

-   **Query Parameters**:
    -   `from`: `string (ISO 8601 datetime with timezone, optional)` - 조회 시작 시점 (기본값: 현재 시각)

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": [
            {
                "id": "string (UUID)",
                "memberId": "string (UUID)",
                "storyboardId": "string (UUID)",
                "scheduledAt": "string (ISO 8601 datetime)",
                "createdAt": "string (ISO 8601 datetime)"
            }
        ]
    }
    ```

### 1.4 인터뷰 완료 표시

#### `PATCH /api/v0/reservation/interview/{interviewId}/done`

특정 인터뷰를 완료 상태로 표시합니다.

-   **Path Variables**:
    -   `interviewId`: `string (UUID)` - 완료 표시할 인터뷰 ID

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": null
    }
    ```

-   **Error Response (500 Internal Server Error)**:
    ```json
    {
        "success": false,
        "message": "인터뷰 완료 처리 중 오류가 발생했습니다.",
        "error": {
            "code": "UNKNOWN",
            "message": "Failed to mark interview as done"
        }
    }
    ```

---

## 2. 리캡(Recap) 예약 관련 API

### 2.1 리캡 예약 생성

#### `POST /api/v0/reservation/recap/video`

특정 영상에 대한 리캡 생성을 예약합니다. 이 API는 복잡한 미디어 처리 파이프라인을 통해 영상에서 오디오를 추출하고 외부 AI 서버에 요청하여 리캡을 생성합니다.

-   **Authentication**: JWT 토큰 필요
-   **Request Body**:
    ```json
    {
        "videoId": "string (UUID)",
        "scheduledAt": "string (ISO 8601 datetime with timezone)"
    }
    ```

-   **Success Response (201 Created)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": {
            "id": "string (UUID)",
            "memberId": "string (UUID)",
            "videoId": "string (UUID)",
            "scheduledAt": "string (ISO 8601 datetime)",
            "createdAt": "string (ISO 8601 datetime)"
        }
    }
    ```

-   **Error Response (500 Internal Server Error)**:
    ```json
    {
        "success": false,
        "message": "리캡 예약 생성 중 오류가 발생했습니다.",
        "error": {
            "code": "UNKNOWN",
            "message": "Failed to create recap reservation"
        }
    }
    ```

**처리 과정:**
1. 예약 정보 DB 저장 (`recap_reservation` 테이블)
2. 영상에서 오디오 추출 (MP4 → WAV)
3. 오디오 압축 (WAV → Opus)
4. S3에 압축된 오디오 업로드
5. 외부 AI 서버에 리캡 생성 요청
6. 결과 저장 (`recap_result` 테이블)

**주요 특징:**
- 임시 파일 안전 관리 (자동 정리)
- 오디오 포맷 최적화 (Opus 압축)
- 외부 AI 서버 연동
- 비동기 처리 (즉시 예약 ID 반환)

### 2.2 리캡 결과 조회

#### `GET /api/v0/reservation/recap/{recapReservationId}/result`

특정 리캡 예약 ID로 생성된 리캡 결과를 조회합니다.

-   **Path Variables**:
    -   `recapReservationId`: `string (UUID)` - 리캡 예약 ID

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": {
            "recap_result_id": "string (UUID)",
            "created_at": "string (ISO 8601 datetime with timezone)",
            "answer_summaries": [
                {
                    "scene_id": "string (UUID)",
                    "question": "string",
                    "answer_summary": "string"
                }
            ]
        }
    }
    ```

-   **Not Found Response (404 Not Found)**:
    ```json
    {
        "success": false,
        "message": "리캡 결과를 찾을 수 없습니다.",
        "error": {
            "code": "NOT_FOUND",
            "message": "Recap result not found"
        }
    }
    ```

---

## 3. 공통 사항

### 인증 요구사항
- 모든 API는 JWT 기반 인증이 필요합니다.
- 요청 헤더에 `Authorization: Bearer {ACCESS_TOKEN}`을 포함해야 합니다.

### 에러 응답 
- 모든 API는 예외 발생 시 500 에러와 함께 `UNKNOWN` 에러 코드를 반환합니다.
- 클라이언트는 적절한 에러 처리를 구현해야 합니다.

### 시간 형식
- 모든 날짜/시간 데이터는 ISO 8601 형식을 사용합니다.
- 요청 시에는 타임존 정보를 포함해야 합니다.
- 응답에서는 서버 로컬 시간 또는 UTC 기준으로 반환됩니다.
