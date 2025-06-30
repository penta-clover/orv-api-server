# ✍️ 코딩 스타일 및 네이밍 규칙

이 문서는 `orv-api-server` 프로젝트의 코드 일관성을 유지하기 위한 코딩 스타일과 네이밍 규칙을 정의합니다. 모든 코드 기여는 이 규칙을 따라야 합니다.

---

### 1. Java 코드 스타일

#### **포맷팅 (Formatting)**

-   **들여쓰기(Indentation)**: 4개의 스페이스(space)를 사용합니다. 탭(tab)은 사용하지 않습니다.
-   **한 줄의 길이**: 120자를 넘지 않도록 합니다.
-   **중괄호(Braces)**: `if`, `for`, `while` 등 모든 제어문에서 중괄호를 생략하지 않습니다. K&R 스타일(여는 중괄호는 같은 줄에, 닫는 중괄호는 새 줄에)을 따릅니다.
    ```java
    // Good
    if (condition) {
        // do something
    }

    // Bad
    if (condition)
        // do something
    ```

#### **임포트 (Imports)**

-   와일드카드(`*`) 임포트는 사용하지 않습니다. (예: `import java.util.*;` 금지)
-   `import` 문은 다음 순서로 그룹화하여 정렬합니다.
    1.  `java`, `javax`
    2.  `org`, `com` 등 외부 라이브러리
    3.  `com.orv.api` (프로젝트 내부 클래스)
-   그룹 사이에는 한 줄의 공백을 둡니다.

#### **기타**

-   `final` 키워드를 적극적으로 사용합니다. 변경되지 않는 지역 변수, 파라미터, 필드에는 `final`을 붙여 불변성을 높입니다.
-   접근 제어자(Access Modifiers)는 항상 명시적으로 선언합니다.

---

### 2. 네이밍 규칙 (Naming Conventions)

일관된 네이밍은 코드의 가독성을 크게 향상시킵니다.

#### **패키지 (Packages)**

-   소문자와 점(`.`)을 사용하여 계층적으로 구성합니다.
-   예: `com.orv.api.domain.auth`

#### **클래스 및 인터페이스 (Classes & Interfaces)**

-   **클래스**: 파스칼 케이스(PascalCase)를 사용합니다.
    -   예: `MemberService`, `ArchiveController`
-   **인터페이스**: 클래스와 동일하게 파스칼 케이스를 사용합니다. `I` 접두사나 `Impl` 접미사를 사용하지 않습니다.
    -   예: `SocialAuthService` (Good), `ISocialAuthService` (Bad)
    -   구현체: `GoogleAuthService`, `KakaoAuthService` (Good), `SocialAuthServiceImpl` (Bad)

#### **메소드 (Methods)**

-   카멜 케이스(camelCase)를 사용합니다.
-   메소드 이름은 동사로 시작하여 무엇을 하는지 명확히 나타내야 합니다.
-   예: `getMemberById`, `createReservation`

#### **변수 (Variables)**

-   카멜 케이스(camelCase)를 사용합니다.
-   **상수(Constants)**: `static final` 필드는 대문자와 언더스코어(`_`)를 사용합니다. (SNAKE_CASE)
    -   예: `public static final int MAX_LOGIN_ATTEMPTS = 5;`

#### **DTO (Data Transfer Objects)**

-   요청(Request)과 응답(Response) DTO를 명확히 구분합니다.
-   **Request DTO**: `{동사}Request` 또는 `{자원}CreateRequest` 형식
    -   예: `LoginRequest`, `MemberCreateRequest`
-   **Response DTO**: `{자원}Response` 또는 `{자원}Info` 형식
    -   예: `MemberResponse`, `TopicInfo`

#### **테스트 클래스 (Test Classes)**

-   테스트 대상 클래스 이름 뒤에 `Test`를 붙입니다.
-   예: `MemberService` → `MemberServiceTest`
-   테스트 메소드 이름은 한글 또는 영문으로 자유롭게 작성하되, `[행위]_[상태]` 또는 `[상황]_[예상결과]` 형식을 권장합니다.
    -   예: `회원가입_성공()`, `존재하지_않는_이메일로_로그인시_예외발생()`
    -   예: `login_withValidCredentials_returnsTokens()`
