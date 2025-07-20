# 🚀 로컬 개발 환경 설정

이 문서는 `orv-api-server` 프로젝트를 로컬 머신에서 실행하기 위한 개발 환경 설정 절차를 안내합니다.

---

### 1. 사전 요구사항 (Prerequisites)

아래의 소프트웨어들이 설치되어 있어야 합니다.

| 소프트웨어 | 권장 버전 | 설치 가이드 |
|---|---|---|
| **Java (JDK)** | 17 | [OpenJDK](https://jdk.java.net/17/) 또는 [SDKMAN!](https://sdkman.io/) |
| **Gradle** | 8.x | 프로젝트에 포함된 Gradle Wrapper가 자동으로 처리 |
| **Docker** | 최신 버전 | [Docker Desktop](https://www.docker.com/products/docker-desktop/) |

---

### 2. 프로젝트 설정 단계

#### **단계 1: 프로젝트 클론**

먼저, Git을 사용하여 프로젝트 소스 코드를 로컬 머신으로 복제합니다.

```bash
git clone {저장소_URL}
cd orv-api-server
```

#### **단계 2: 로컬 개발 환경 실행 (Docker)**

이 프로젝트는 PostgreSQL 데이터베이스와 LocalStack(로컬 AWS S3 환경)을 Docker 컨테이너로 실행합니다. `docker/dev/docker-compose.yaml` 파일에 모든 설정이 포함되어 있습니다.

아래 명령어를 프로젝트 루트 디렉토리에서 실행하여 컨테이너들을 백그라운드에서 실행합니다.

```bash
docker-compose -f docker/dev/docker-compose.yaml up -d
```

**실행 확인:**
- `docker ps` 명령어를 실행하여 `postgres`와 `localstack` 컨테이너가 정상적으로 실행 중인지 확인합니다.

#### **단계 3: 애플리케이션 환경변수 설정**

`src/main/resources/application.properties` 파일은 기본적으로 로컬 개발 환경에 맞게 설정되어 있습니다. 하지만 민감한 정보(예: JWT 시크릿 키)는 직접 추가해야 할 수 있습니다.

**주요 설정값:**
- **데이터베이스 연결**:
  - `spring.datasource.url=jdbc:postgresql://localhost:5432/orv-local`
  - `spring.datasource.username=orv-user`
  - `spring.datasource.password=orv-password`
- **Flyway**:
  - `spring.flyway.enabled=true` (애플리케이션 시작 시 자동으로 DB 마이그레이션 실행)
- **LocalStack S3**:
  - `cloud.aws.s3.endpoint=http://localhost:4566`
  - `cloud.aws.credentials.access-key=test`
  - `cloud.aws.credentials.secret-key=test`
  - `cloud.aws.region.static=ap-northeast-2`

> **참고**: 만약 별도의 환경변수 파일(`.env`)을 사용한다면, 해당 파일에 민감한 정보를 설정하고 `.gitignore`에 추가하여 버전 관리에서 제외해야 합니다.

#### **단계 4: Gradle 의존성 설치 및 빌드**

프로젝트 루트 디렉토리에서 아래 명령어를 실행하여 필요한 라이브러리를 다운로드하고 프로젝트를 빌드합니다.

```bash
./gradlew build
```

이 과정에서 테스트도 함께 실행됩니다. 만약 테스트를 건너뛰고 싶다면 아래 명령어를 사용합니다.

```bash
./gradlew build -x test
```

---

### 3. 애플리케이션 실행

모든 설정이 완료되었다면, 이제 애플리케이션을 실행할 수 있습니다.

#### **방법 1: IDE에서 실행 (권장)**

- IntelliJ IDEA 또는 Eclipse와 같은 IDE에서 프로젝트를 엽니다.
- `src/main/java/com/orv/api/ApiApplication.java` 파일을 찾습니다.
- `main` 메소드 옆의 실행 버튼을 클릭하여 애플리케이션을 시작합니다.

#### **방법 2: Gradle을 통해 CLI에서 실행**

프로젝트 루트 디렉토리에서 아래 명령어를 실행합니다.

```bash
./gradlew bootRun
```

애플리케이션이 성공적으로 시작되면, 콘솔 로그에 `Started ApiApplication in ... seconds`와 같은 메시지가 출력됩니다. 이제 API 서버는 `http://localhost:8080`에서 요청을 받을 준비가 되었습니다.
