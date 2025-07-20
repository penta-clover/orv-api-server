# 🔍 주요 기능별 디버깅 팁

이 문서는 프로젝트의 주요 기능에서 문제가 발생했을 때 효과적으로 디버깅할 수 있는 팁과 체크리스트를 제공합니다.

---

### 1. 인증/인가 (JWT) 문제

#### **증상**:
-   API 요청 시 401 Unauthorized 또는 403 Forbidden 오류가 발생한다.
-   로그인은 성공했지만, 인증이 필요한 다른 API 호출에 계속 실패한다.

#### **체크리스트 및 디버깅 팁**:

1.  **`Authorization` 헤더 확인**:
    -   클라이언트가 보낸 요청 헤더에 `Authorization: Bearer {TOKEN}` 형식이 올바르게 포함되었는지 확인합니다. `Bearer`와 토큰 사이에 공백이 있는지 확인하세요.

2.  **JWT 토큰 디코딩**:
    -   [jwt.io](https://jwt.io/)와 같은 온라인 디버거를 사용하여 클라이언트가 보낸 Access Token을 붙여넣고 내용을 확인합니다.
        -   `payload` 부분에 `sub`(사용자 ID), `roles`, `exp`(만료 시간) 등의 정보가 올바르게 들어있는지 확인합니다.
        -   `exp` 시간이 현재 시간보다 이전인지 확인하여 토큰이 만료되지 않았는지 검사합니다.

3.  **`JwtAuthorizationFilter` 디버깅**:
    -   IDE에서 `JwtAuthorizationFilter` 클래스의 `doFilterInternal` 메소드에 브레이크포인트(breakpoint)를 설정합니다.
    -   API를 요청하여 프로그램 실행을 일시 중지시키고, 아래 변수들의 값을 단계별로 확인합니다.
        -   `header`: `Authorization` 헤더 값이 제대로 들어오는지 확인.
        -   `jwtToken`: `Bearer` 접두사가 제거된 순수 토큰 문자열이 맞는지 확인.
        -   `authentication`: `JwtTokenProvider`가 토큰을 성공적으로 파싱하여 `Authentication` 객체를 생성하는지 확인.

4.  **`SecurityConfig` 확인**:
    -   문제가 발생하는 API의 경로가 `SecurityConfig`의 `authorizeHttpRequests` 설정에서 올바른 접근 권한을 가지고 있는지 확인합니다. (예: `permitAll`, `hasRole('ADMIN')`, `authenticated`)

---

### 2. AWS S3 / LocalStack 연동 문제

#### **증상**:
-   파일 업로드/다운로드 시 `SdkClientException: Unable to execute HTTP request` 오류가 발생한다.
-   파일은 업로드된 것처럼 보이나, 실제 S3 버킷이나 LocalStack에서 파일을 찾을 수 없다.

#### **체크리스트 및 디버깅 팁**:

1.  **LocalStack 컨테이너 상태 확인**:
    -   `docker ps` 명령어로 `localstack` 컨테이너가 정상적으로 실행 중인지 확인합니다.
    -   LocalStack의 Web UI(`http://localhost:8080` 또는 다른 포트)에 접속하여 S3 서비스가 활성화되어 있는지 확인합니다.

2.  **`application.properties` 설정 확인**:
    -   `cloud.aws.s3.endpoint`가 LocalStack 주소(`http://localhost:4566`)로 올바르게 설정되었는지 확인합니다.
    -   `cloud.aws.credentials.access-key`와 `secret-key`가 `test`로 설정되어 있는지 확인합니다.
    -   `cloud.aws.region.static` 값이 올바르게 설정되었는지 확인합니다.

3.  **S3 버킷 생성 확인**:
    -   애플리케이션이 시작될 때 `S3Config` 또는 관련 로직에서 설정된 버킷(`orv-dev-bucket` 등)이 LocalStack에 실제로 생성되었는지 확인해야 합니다. LocalStack CLI 또는 Web UI를 사용하여 버킷 목록을 조회합니다.
    -   **팁**: LocalStack은 컨테이너가 재시작될 때마다 데이터가 초기화될 수 있습니다. 필요하다면 LocalStack이 시작될 때 특정 버킷을 자동으로 생성하도록 초기화 스크립트를 구성할 수 있습니다.

4.  **`S3VideoRepository` (또는 관련 클래스) 디버깅**:
    -   파일을 업로드하는 로직(예: `putObject`)에 브레이크포인트를 설정합니다.
    -   `PutObjectRequest` 객체에 `bucket`, `key`(파일 경로), `inputStream` 등의 값이 올바르게 채워지는지 확인합니다.
