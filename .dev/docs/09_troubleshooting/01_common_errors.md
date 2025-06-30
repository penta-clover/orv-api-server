# 🔍 자주 발생하는 오류 및 해결 가이드

이 문서는 프로젝트 개발 및 실행 과정에서 자주 발생하는 일반적인 오류와 그에 대한 해결 방법을 안내합니다.

---

### 1. 빌드/의존성 관련 오류

#### **오류 메시지**: `Could not resolve dependencies...` 또는 `Task :compileJava FAILED`

-   **원인**:
    1.  Gradle이 필요한 라이브러리(의존성)를 원격 저장소(Maven Central, JCenter 등)에서 다운로드하지 못했습니다.
    2.  `build.gradle` 파일에 문법 오류가 있거나 잘못된 의존성이 선언되었습니다.
    3.  인터넷 연결이 불안정하거나, 사내 방화벽 등으로 인해 원격 저장소 접근이 차단되었습니다.

-   **해결 방법**:
    1.  **Gradle 캐시 정리 및 재시도**: 아래 명령어를 실행하여 Gradle 캐시를 삭제하고 다시 빌드합니다.
        ```bash
        # macOS / Linux
        rm -rf ~/.gradle/caches/
        ./gradlew clean build

        # Windows
        del /s /q "%USERPROFILE%\.gradle\caches"
        gradlew.bat clean build
        ```
    2.  **`build.gradle` 파일 확인**: 최근에 수정한 `build.gradle` 파일의 내용을 검토하여 오타나 잘못된 버전 정보가 없는지 확인합니다.
    3.  **네트워크 확인**: 인터넷 연결 상태를 확인하고, 프록시나 VPN을 사용 중이라면 잠시 비활성화한 후 다시 시도합니다.

---

### 2. 데이터베이스 관련 오류

#### **오류 메시지**: `FATAL: password authentication failed for user "orv-user"`

-   **원인**: `application.properties`에 설정된 데이터베이스 비밀번호가 로컬 Docker 컨테이너의 PostgreSQL에 설정된 비밀번호와 일치하지 않습니다.
-   **해결 방법**:
    -   `application.properties`의 `spring.datasource.password` 값이 `docker/dev/docker-compose.yaml` 파일에 설정된 `POSTGRES_PASSWORD` 값(`orv-password`)과 동일한지 확인합니다.

#### **오류 메시지**: `FlywayException: Validate failed: Migration checksum mismatch for...`

-   **원인**: 이미 데이터베이스에 적용된 Flyway 마이그레이션 SQL 파일의 내용이 **수정**되었습니다. Flyway는 보안을 위해 한 번 적용된 파일의 변경을 허용하지 않습니다.
-   **해결 방법**:
    1.  **절대 기존 마이그레이션 파일을 수정하지 마십시오.**
    2.  로컬 개발 환경이라면, 가장 간단한 해결책은 데이터베이스 컨테이너를 완전히 삭제하고 다시 생성하는 것입니다.
        ```bash
        # 1. 컨테이너 중지 및 삭제
        docker-compose -f docker/dev/docker-compose.yaml down

        # 2. (중요) 볼륨 삭제하여 데이터 초기화
        docker volume rm orv-api-server_postgres_data

        # 3. 컨테이너 다시 생성 및 시작
        docker-compose -f docker/dev/docker-compose.yaml up -d
        ```
    3.  운영 환경에서 이런 문제가 발생했다면, 문제를 일으킨 커밋을 되돌리고(revert), 올바른 내용을 담은 **새로운** 마이그레이션 파일을 작성하여 배포해야 합니다.

---

### 3. 애플리케이션 실행 오류

#### **오류 메시지**: `Web server failed to start. Port 8080 was already in use.`

-   **원인**: 로컬 머신의 8080 포트를 다른 프로세스가 이미 사용하고 있습니다. (예: 이전에 종료되지 않은 다른 `orv-api-server` 프로세스)
-   **해결 방법**:
    -   **8080 포트를 사용하는 프로세스 찾아서 종료하기**:
        ```bash
        # macOS / Linux
        lsof -i :8080
        # 위 명령어로 PID (프로세스 ID) 확인 후
        kill -9 {PID}

        # Windows
        netstat -ano | findstr :8080
        # 위 명령어로 PID 확인 후
        taskkill /F /PID {PID}
