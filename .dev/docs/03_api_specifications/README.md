# 📋 API 명세 (API Specifications)

이 문서는 `orv-api-server`가 제공하는 API의 공통 규칙과 전체 엔드포인트 목록을 안내합니다. 각 도메인별 상세 명세는 해당 디렉토리의 개별 `md` 파일에 정의되어 있습니다.

> 이 문서는 `src/docs/asciidoc`에 있는 기존 API 문서를 기반으로 작성되었으며, AI 에이전트와 개발자가 사용하기 용이한 마크다운 형식으로 재구성되었습니다.

---

### 1. 공통 규칙 (Common Rules)

#### **Base URL**

모든 API 요청의 기본 경로는 아래와 같습니다.

-   **로컬 환경**: `http://localhost:8080`
-   **개발/운영 환경**: `https://{배포_도메인}`

#### **인증 (Authentication)**

-   로그인/회원가입 등 일부 공개 API를 제외한 모든 API는 인증이 필요합니다.
-   인증 방식은 **JWT (JSON Web Token)**를 사용합니다.
-   API 요청 시, HTTP 헤더에 아래와 같이 `Authorization` 토큰을 포함해야 합니다.

```
Authorization: Bearer {ACCESS_TOKEN}
```

-   Access Token이 만료된 경우, Refresh Token을 사용하여 새로운 토큰을 발급받아야 합니다.

#### **요청 형식 (Request Format)**

-   모든 `POST`, `PUT`, `PATCH` 요청의 본문(body)은 `application/json` 형식의 JSON 데이터를 사용합니다.

---

### 2. 공통 응답 구조 (Common Response Structure)

모든 API 응답은 아래와 같은 표준화된 구조를 따릅니다.

#### **성공 (Success)**

```json
{
    "success": true,
    "message": "요청에 성공했습니다.",
    "data": {
        // 실제 응답 데이터
    }
}
```

-   `success`: 항상 `true`
-   `message`: 응답에 대한 요약 메시지
-   `data`: 실제 반환되는 데이터 객체. 데이터가 없는 경우 `null` 또는 빈 객체 `{}`

#### **실패 (Error)**

```json
{
    "success": false,
    "message": "요청 처리 중 오류가 발생했습니다.",
    "error": {
        "code": "ERROR_CODE",
        "message": "상세 오류 메시지"
    }
}
```

-   `success`: 항상 `false`
-   `message`: 오류 발생에 대한 요약 메시지
-   `error`: 오류에 대한 상세 정보를 담는 객체
    -   `code`: 오류를 식별할 수 있는 고유 코드 (예: `INVALID_INPUT`, `AUTHENTICATION_FAILED`)
    -   `message`: 개발자가 이해할 수 있는 상세 오류 원인

---

### 3. 도메인별 API 명세 목록

-   [인증/회원 (Auth/Member)](./auth.md)
-   아카이브 (Archive) - (작성 예정)
-   예약 (Reservation) - (작성 예정)
-   스토리보드 (Storyboard) - (작성 예정)
-   약관 (Term) - (작성 예정)
