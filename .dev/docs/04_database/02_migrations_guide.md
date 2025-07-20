# 🗄️ 데이터베이스 마이그레이션 가이드 (Flyway)

이 문서는 Flyway를 사용하여 데이터베이스 스키마를 안전하고 일관성 있게 변경하는 절차와 규칙을 설명합니다. **데이터베이스 스키마와 관련된 모든 변경은 반드시 이 가이드를 따라야 합니다.**

---

### 1. Flyway 기본 원칙

-   **모든 스키마 변경은 마이그레이션 스크립트를 통해 이루어져야 합니다.** 데이터베이스에 직접 `ALTER TABLE`, `CREATE TABLE` 등의 DDL을 실행해서는 안 됩니다.
-   **한 번 적용된(merged) 마이그레이션 파일은 절대 수정해서는 안 됩니다.** 이미 다른 개발자나 운영 서버에 적용되었을 수 있기 때문입니다. 잘못된 내용을 수정하려면, 새로운 마이그레이션 스크립트를 작성하여 변경 사항을 적용해야 합니다.

---

### 2. 마이그레이션 스크립트 작성 방법

#### **단계 1: 파일 생성**

-   **경로**: `src/main/resources/db/migration`
-   **파일명 규칙**: `V{버전}__{설명}.sql`
    -   `V`: Versioned Migration을 의미하는 접두사 (필수)
    -   `{버전}`: `Major.Minor.Patch` 형식의 버전 번호. 기존 마이그레이션 파일들의 버전을 확인하고 그 다음 번호를 사용합니다. (예: `V1.0.11`)
    -   `__`: (언더스코어 두 개) 버전과 설명을 구분하는 구분자 (필수)
    -   `{설명}`: 이 마이그레이션이 어떤 작업을 하는지 간결하게 설명합니다. 단어는 언더스코어(`_`)로 연결합니다. (예: `create_user_nickname_column`)

-   **예시**:
    -   `V1.0.12__add_nickname_to_member_table.sql`
    -   `V1.1.0__create_new_feature_table.sql`

#### **단계 2: SQL 작성**

-   생성한 SQL 파일에 실행하고자 하는 DDL(Data Definition Language) 쿼리를 작성합니다.
-   SQL 문법은 PostgreSQL 표준을 따릅니다.
-   하나의 파일에는 논리적으로 관련된 DDL 작업들을 함께 포함하는 것이 좋습니다.

**예시 (`V1.0.12__add_nickname_to_member_table.sql`):**
```sql
-- MEMBER 테이블에 nickname 컬럼 추가
ALTER TABLE member
ADD COLUMN nickname VARCHAR(50);

-- nickname 컬럼에 중복을 방지하기 위한 UNIQUE 인덱스 추가
CREATE UNIQUE INDEX idx_member_nickname ON member (nickname);

-- 기존 데이터의 nickname을 'user_{id}' 형식으로 초기화
UPDATE member
SET nickname = 'user_' || id
WHERE nickname IS NULL;

-- nickname 컬럼에 NOT NULL 제약 조건 추가
ALTER TABLE member
ALTER COLUMN nickname SET NOT NULL;
```

---

### 3. 마이그레이션 실행 및 검증

#### **로컬 환경**

1.  **자동 실행**: 로컬에서 Spring Boot 애플리케이션을 실행하면(`bootRun`), Flyway가 자동으로 활성화되어 아직 적용되지 않은 새로운 마이그레이션 스크립트를 찾아 데이터베이스에 적용합니다.
2.  **로그 확인**: 애플리케이션 시작 로그에서 Flyway가 마이그레이션을 성공적으로 수행했는지 확인합니다.
    ```
    ... Successfully applied 1 migration to schema "public" (execution time 00:00.015s)
    ```
3.  **DB 확인**: DBeaver, pgAdmin과 같은 데이터베이스 클라이언트를 사용하여 실제 스키마가 의도한 대로 변경되었는지 확인합니다.

#### **운영 환경**

-   운영 환경에서는 CI/CD 파이프라인의 배포 단계에서 애플리케이션이 시작될 때 로컬 환경과 동일하게 Flyway 마이그레이션이 자동으로 수행됩니다.

---

### 4. 롤백 (Rollback) - 주의!

-   Flyway의 무료 버전(Community Edition)은 자동 롤백을 지원하지 않습니다.
-   만약 잘못된 마이그레이션을 적용했다면, **새로운 마이그레이션 스크립트를 작성하여 문제를 해결해야 합니다.**
    -   **예시**: 컬럼을 잘못 추가했다면, 해당 컬럼을 삭제(`DROP COLUMN`)하는 내용의 새로운 마이그레이션 스크립트를 작성합니다.
