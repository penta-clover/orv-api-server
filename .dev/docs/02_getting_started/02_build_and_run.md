# 🚀 빌드 및 실행 가이드

이 문서는 프로젝트를 빌드, 테스트, 실행하는 데 사용되는 주요 Gradle 명령어들을 설명합니다. 모든 명령어는 프로젝트의 루트 디렉토리에서 실행해야 합니다.

---

### 1. 주요 빌드 명령어

#### **전체 빌드 (Clean & Build)**

가장 표준적인 빌드 명령어입니다. 기존 빌드 산출물(`build` 디렉토리)을 삭제하고, 전체 코드를 다시 컴파일하며, 모든 테스트를 실행한 후, 실행 가능한 JAR 파일을 생성합니다.

```bash
./gradlew clean build
```

-   **`clean`**: `build` 디렉토리를 삭제하여 이전 빌드의 결과물이 다음 빌드에 영향을 주지 않도록 합니다.
-   **`build`**: 컴파일, 테스트, 패키징 등 빌드에 필요한 모든 작업을 순서대로 실행합니다.

**산출물**: 빌드가 성공적으로 완료되면 `build/libs` 디렉토리에 실행 가능한 `.jar` 파일이 생성됩니다.

---

### 2. 테스트 관련 명령어

#### **모든 테스트 실행**

`build` 명령어에 포함되어 있지만, 테스트만 독립적으로 실행하고 싶을 때 사용합니다.

```bash
./gradlew test
```

#### **특정 테스트 실행**

특정 클래스, 메소드, 또는 패키지의 테스트만 실행할 수 있어 개발 및 디버깅 시 유용합니다.

```bash
# 특정 테스트 클래스 실행
./gradlew test --tests "com.orv.api.unit.domain.auth.MemberServiceTest"

# 특정 테스트 메소드 실행
./gradlew test --tests "com.orv.api.unit.domain.auth.MemberServiceTest.회원가입_성공"

# 특정 패키지의 모든 테스트 실행
./gradlew test --tests "com.orv.api.integration.domain.auth.*"
```

#### **테스트 건너뛰고 빌드**

로컬에서 빠르게 빌드 결과물만 확인하고 싶을 때 사용합니다.

```bash
./gradlew build -x test
```

---

### 3. 애플리케이션 실행 명령어

#### **로컬 서버 실행 (개발 모드)**

Spring Boot 개발 도구(`spring-boot-devtools`)가 활성화되어 있어, 코드 변경 시 자동으로 애플리케이션이 재시작됩니다. 개발 시 가장 유용합니다.

```bash
./gradlew bootRun
```

#### **빌드된 JAR 파일 실행 (운영 환경과 유사)**

`./gradlew build` 명령어로 생성된 JAR 파일을 직접 실행합니다. 운영 환경에서 애플리케이션을 배포하고 실행하는 방식과 가장 유사합니다.

```bash
java -jar build/libs/orv-api-server-0.0.1-SNAPSHOT.jar
```

> **참고**: 위 명령어의 파일명(`orv-api-server-0.0.1-SNAPSHOT.jar`)은 `build.gradle`의 버전 설정에 따라 달라질 수 있습니다.

---

### 4. 기타 유용한 명령어

#### **의존성 확인**

프로젝트의 전체 의존성 트리를 확인하고 싶을 때 사용합니다. 의존성 충돌 문제를 해결하는 데 도움이 됩니다.

```bash
./gradlew dependencies
```

#### **Gradle 작업 목록 확인**

현재 프로젝트에서 실행 가능한 모든 Gradle 작업(task)의 목록을 보여줍니다.

```bash
./gradlew tasks
