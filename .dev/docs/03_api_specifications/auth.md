# 📋 API 명세: 인증/회원 (Auth/Member)

이 문서는 인증(소셜 로그인, 토큰 재발급) 및 회원 정보 관련 API의 상세 명세를 정의합니다.

---

### 1. 소셜 로그인

#### `POST /api/v1/auth/login/{provider}`

지정된 소셜 미디어 제공자(provider)의 ID 토큰을 사용하여 로그인 또는 회원가입을 처리하고, 서비스의 Access Token과 Refresh Token을 발급합니다.

-   **Path Variables**:
    -   `provider`: `google` 또는 `kakao`

-   **Request Body**:
    ```json
    {
        "idToken": "string (The ID token provided by the social media platform)"
    }
    ```

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": {
            "isNewMember": boolean,
            "accessToken": "string (JWT)",
            "refreshToken": "string (JWT)"
        }
    }
    ```
    - `isNewMember`: 해당 소셜 정보로 새로 가입한 사용자인 경우 `true`

-   **Error Response (401 Unauthorized)**:
    - ID 토큰이 유효하지 않을 경우.
    ```json
    {
        "success": false,
        "message": "유효하지 않은 ID 토큰입니다.",
        "error": {
            "code": "INVALID_ID_TOKEN",
            "message": "The provided ID token is invalid or expired."
        }
    }
    ```

---

### 2. 토큰 재발급

#### `POST /api/v1/auth/refresh`

만료된 Access Token을 새로운 토큰으로 재발급합니다. 요청 시 유효한 Refresh Token을 헤더에 포함해야 합니다.

-   **Headers**:
    -   `Authorization`: `Bearer {REFRESH_TOKEN}`

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": {
            "accessToken": "string (New JWT)",
            "refreshToken": "string (New JWT)"
        }
    }
    ```

-   **Error Response (401 Unauthorized)**:
    - Refresh Token이 유효하지 않거나 만료된 경우.
    ```json
    {
        "success": false,
        "message": "인증에 실패했습니다.",
        "error": {
            "code": "INVALID_REFRESH_TOKEN",
            "message": "The provided refresh token is invalid or expired."
        }
    }
    ```

---

### 3. 내 정보 조회

#### `GET /api/v1/members/me`

현재 로그인된 사용자의 정보를 조회합니다. Access Token을 헤더에 포함해야 합니다.

-   **Headers**:
    -   `Authorization`: `Bearer {ACCESS_TOKEN}`

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "요청에 성공했습니다.",
        "data": {
            "id": "integer",
            "name": "string",
            "email": "string",
            "profileImageUrl": "string",
            "provider": "string (e.g., 'google', 'kakao')"
        }
    }
    ```

---

### 4. 회원 탈퇴

#### `DELETE /api/v1/members/me`

현재 로그인된 사용자의 정보를 삭제하고 서비스를 탈퇴 처리합니다. Access Token을 헤더에 포함해야 합니다.

-   **Headers**:
    -   `Authorization`: `Bearer {ACCESS_TOKEN}`

-   **Success Response (200 OK)**:
    ```json
    {
        "success": true,
        "message": "회원 탈퇴가 성공적으로 처리되었습니다.",
        "data": null
    }
