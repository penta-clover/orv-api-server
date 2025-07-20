# 📋 API 명세: 스토리보드/주제 (Storyboard/Topic)

이 문서는 스토리보드, 씬(Scene), 주제(Topic) 관련 API의 상세 명세를 정의합니다.

---

## 1. 스토리보드 관련 API

### 1.1 스토리보드 조회

#### `GET /api/v0/storyboard/{storyboardId}`

특정 스토리보드의 기본 정보를 조회합니다.

-   **Path Variables**:
    -   `storyboardId`: `string (UUID)` - 조회할 스토리보드 ID

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": {
            "id": "string (UUID)",
            "title": "string",
            "startSceneId": "string (UUID)"
        }
    }
    ```

-   **Not Found Response (404 Not Found)**:
    ```json
    {
        "success": false,
        "message": "스토리보드를 찾을 수 없습니다.",
        "error": {
            "code": "NOT_FOUND",
            "message": "Storyboard not found"
        }
    }
    ```

### 1.2 스토리보드의 모든 씬 조회

#### `GET /api/v0/storyboard/{storyboardId}/scene/all`

특정 스토리보드에 속한 모든 씬(Scene) 목록을 조회합니다.

-   **Path Variables**:
    -   `storyboardId`: `string (UUID)` - 조회할 스토리보드 ID

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": [
            {
                "id": "string (UUID)",
                "name": "string",
                "sceneType": "string",
                "content": "string",
                "storyboardId": "string (UUID)"
            }
        ]
    }
    ```

-   **Error Response (500 Internal Server Error)**:
    ```json
    {
        "success": false,
        "message": "씬 조회 중 오류가 발생했습니다.",
        "error": {
            "code": "UNKNOWN",
            "message": "Failed to retrieve scenes"
        }
    }
    ```

### 1.3 특정 씬 조회

#### `GET /api/v0/storyboard/scene/{sceneId}`

특정 씬의 상세 정보를 조회합니다. 이 API는 사용자의 사용 이력을 업데이트합니다.

-   **Path Variables**:
    -   `sceneId`: `string (UUID)` - 조회할 씬 ID

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": {
            "id": "string (UUID)",
            "name": "string",
            "sceneType": "string",
            "content": "string",
            "storyboardId": "string (UUID)"
        }
    }
    ```

-   **Not Found Response (404 Not Found)**:
    ```json
    {
        "success": false,
        "message": "씬을 찾을 수 없습니다.",
        "error": {
            "code": "NOT_FOUND",
            "message": "Scene not found"
        }
    }
    ```

### 1.4 스토리보드 미리보기

#### `GET /api/v0/storyboard/{storyboardId}/preview`

스토리보드의 미리보기 정보(질문 개수, 예시 질문들)를 조회합니다.

-   **Path Variables**:
    -   `storyboardId`: `string (UUID)` - 조회할 스토리보드 ID

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": {
            "storyboardId": "string (UUID)",
            "questionCount": "integer",
            "questions": [
                "string"
            ]
        }
    }
    ```

-   **Error Response (404 Not Found)**:
    ```json
    {
        "success": false,
        "message": "스토리보드를 찾을 수 없습니다.",
        "error": {
            "code": "NOT_FOUND",
            "message": "Storyboard not found"
        }
    }
    ```

### 1.5 스토리보드의 주제 목록

#### `GET /api/v0/storyboard/{storyboardId}/topic/list`

특정 스토리보드와 관련된 주제 목록을 조회합니다.

-   **Path Variables**:
    -   `storyboardId`: `string (UUID)` - 조회할 스토리보드 ID

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": [
            {
                "id": "string (UUID)",
                "name": "string",
                "description": "string",
                "thumbnailUrl": "string",
                "hashtags": [
                    {
                        "name": "string",
                        "color": "string"
                    }
                ]
            }
        ]
    }
    ```

---

## 2. 주제(Topic) 관련 API

### 2.1 주제 목록 조회

#### `GET /api/v0/topic/list`

카테고리별로 주제 목록을 조회합니다.

-   **Query Parameters**:
    -   `category`: `string` (optional, default: `"DEFAULT"`) - 카테고리 코드

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": [
            {
                "id": "string (UUID)",
                "name": "string",
                "description": "string",
                "thumbnailUrl": "string",
                "hashtags": [
                    {
                        "name": "string",
                        "color": "string"
                    }
                ]
            }
        ]
    }
    ```

-   **Error Response (500 Internal Server Error)**:
    ```json
    {
        "success": false,
        "message": "주제 목록 조회 중 오류가 발생했습니다.",
        "error": {
            "code": "UNKNOWN",
            "message": "Failed to retrieve topics"
        }
    }
    ```

### 2.2 특정 주제 조회

#### `GET /api/v0/topic/{topicId}`

특정 주제의 상세 정보를 조회합니다.

-   **Path Variables**:
    -   `topicId`: `string (UUID)` - 조회할 주제 ID

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": {
            "id": "string (UUID)",
            "name": "string",
            "description": "string",
            "thumbnailUrl": "string",
            "hashtags": [
                {
                    "name": "string",
                    "color": "string"
                }
            ]
        }
    }
    ```

-   **Not Found Response (404 Not Found)**:
    ```json
    {
        "success": false,
        "message": "주제를 찾을 수 없습니다.",
        "error": {
            "code": "NOT_FOUND",
            "message": "Topic not found"
        }
    }
    ```

### 2.3 주제의 다음 스토리보드 조회

#### `GET /api/v0/topic/{topicId}/storyboard/next`

특정 주제에 대한 다음 스토리보드를 조회합니다.

-   **Path Variables**:
    -   `topicId`: `string (UUID)` - 조회할 주제 ID

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": {
            "id": "string (UUID)",
            "title": "string",
            "startSceneId": "string (UUID)"
        }
    }
    ```

-   **Not Found Response (404 Not Found)**:
    ```json
    {
        "success": false,
        "message": "스토리보드를 찾을 수 없습니다.",
        "error": {
            "code": "NOT_FOUND",
            "message": "No storyboard found for this topic"
        }
    }
    ```

---

## 3. 데이터 모델 상세

### 3.1 Storyboard
```json
{
    "id": "string (UUID)",
    "title": "string",
    "startSceneId": "string (UUID)"
}
```

### 3.2 Scene
```json
{
    "id": "string (UUID)",
    "name": "string",
    "sceneType": "string (QUESTION | END | etc.)",
    "content": "string",
    "storyboardId": "string (UUID)"
}
```

### 3.3 Topic
```json
{
    "id": "string (UUID)",
    "name": "string",
    "description": "string",
    "thumbnailUrl": "string",
    "hashtags": [
        {
            "name": "string",
            "color": "string"
        }
    ]
}
```

### 3.4 Hashtag
```json
{
    "name": "string",
    "color": "string"
}
```

---

## 4. 공통 사항

### 인증 요구사항
- 씬 조회 API (`GET /api/v0/storyboard/scene/{sceneId}`)는 JWT 기반 인증이 필요합니다.
- 기타 조회 API들은 인증이 필요하지 않을 수 있습니다 (구현에 따라 다름).

### 사용 이력 추적
- 씬 조회 시 사용자의 사용 이력이 자동으로 업데이트됩니다.
- 씬 타입이 "END"인 경우 "COMPLETED" 상태로, 그 외에는 "STARTED" 상태로 기록됩니다.

### 에러 처리
- 모든 API는 예외 발생 시 500 에러와 함께 `UNKNOWN` 에러 코드를 반환합니다.
- 리소스를 찾을 수 없는 경우 404 에러와 함께 `NOT_FOUND` 에러 코드를 반환합니다.
