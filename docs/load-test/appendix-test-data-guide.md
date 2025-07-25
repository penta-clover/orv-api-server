# 테스트 데이터 생성 가이드

> **[← 이전: 모니터링 및 분석](08-monitoring.md)** | **[메인으로 돌아가기](README.md)**

본 문서는 ORV API Server 부하테스트를 위한 테스트 데이터 생성 방법을 상세히 안내합니다. 실제 DB 스키마를 기반으로 하여 6,000명의 테스트 사용자와 관련 데이터를 생성하는 방법을 다룹니다.

---

## 1. 실제 DB 스키마 기반 TestAuthService 구현

### 1.1 TestAuthService 클래스

```java
// src/main/java/com/orv/api/domain/auth/TestAuthService.java
@Component
@ConditionalOnProperty(name = "test.auth.enabled", havingValue = "true")
@Profile({"loadtest", "test"}) // 프로덕션 환경 완전 차단
public class TestAuthService implements SocialAuthService {
    
    private static final String TEST_USER_PREFIX = "test_user_";
    private static final int MAX_TEST_USERS = 6000;
    
    @Override
    public String getAuthorizationUrl(String state) {
        // nGrinder에서는 실제 OAuth 플로우 없이 바로 callback 호출
        return "http://localhost:8080/test-oauth-callback";
    }
    
    @Override
    public SocialUserInfo getUserInfo(String code) {
        // code 형식: "test_user_1", "test_user_2", ..., "test_user_6000"
        if (!code.startsWith(TEST_USER_PREFIX)) {
            throw new IllegalArgumentException("Invalid test auth code: " + code);
        }
        
        try {
            String userIdStr = code.replace(TEST_USER_PREFIX, "");
            int userId = Integer.parseInt(userIdStr);
            
            // 사용자 ID 범위 검증 (1-6000)
            if (userId < 1 || userId > MAX_TEST_USERS) {
                throw new IllegalArgumentException("Test user ID out of range: " + userId);
            }
            
            return SocialUserInfo.builder()
                .provider("test")
                .id("fake_social_id_" + userId)           // social_id (UNIQUE)
                .email("loadtest_" + userId + "@test.com")
                .name("LoadTest" + userId)
                .build();
                
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid test user ID format: " + code);
        }
    }
    
    @Override
    public void revokeToken(String token) {
        // 테스트 환경에서는 토큰 해제 불필요
    }
}
```

### 1.2 SocialAuthServiceFactory 수정

```java
// src/main/java/com/orv/api/domain/auth/SocialAuthServiceFactory.java
@Service
@RequiredArgsConstructor
public class SocialAuthServiceFactory {
    private final GoogleAuthService googleAuthService;
    private final KakaoAuthService kakaoAuthService;
    
    @Autowired(required = false) // 조건부 주입
    private final Optional<TestAuthService> testAuthService;
    
    public SocialAuthService getSocialAuthService(String provider) {
        if ("google".equalsIgnoreCase(provider)) {
            return googleAuthService;
        } else if ("kakao".equalsIgnoreCase(provider)) {
            return kakaoAuthService;
        } else if ("test".equalsIgnoreCase(provider)) {
            if (testAuthService.isPresent()) {
                return testAuthService.get();
            } else {
                throw new IllegalArgumentException("테스트 인증 서비스가 활성화되지 않았습니다.");
            }
        } else {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다: " + provider);
        }
    }
}
```

---

## 2. 실제 DB 스키마 기반 테스트 데이터 생성

### 2.1 기본 테스트 사용자 생성 (6,000명)

```sql
-- 파일명: create_test_users.sql
-- 실행 전 확인: PostgreSQL에서 uuid_generate_v4() 함수 사용 가능한지 확인

-- 1. 기본 역할 생성 (없다면)
INSERT INTO role (id, name) 
VALUES (uuid_generate_v4(), 'USER')
ON CONFLICT (name) DO NOTHING;

-- 2. 6,000명의 테스트 사용자 생성 (실제 스키마 제약사항 반영)
INSERT INTO member (
    id, 
    nickname, 
    provider, 
    social_id, 
    email, 
    profile_image_url,
    name,
    gender,
    phone_number,
    created_at
)
SELECT 
    uuid_generate_v4(),                           -- member 테이블은 uuid_generate_v4() 사용
    'LT' || LPAD(generate_series::text, 6, '0'),  -- LT000001, LT000002, ... (8자 제한)
    'test',
    'fake_social_id_' || generate_series,         -- social_id (provider, social_id 조합 UNIQUE)
    'loadtest_' || generate_series || '@test.com',
    'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-profile.png',
    'LoadTest' || generate_series,
    CASE WHEN generate_series % 2 = 0 THEN 'MALE' ELSE 'FEMALE' END,
    '010-' || LPAD((generate_series % 10000)::text, 4, '0') || '-' || LPAD((generate_series % 10000)::text, 4, '0'),
    CURRENT_TIMESTAMP - INTERVAL '1 day' * (random() * 365)  -- 지난 1년 내 랜덤 가입일
FROM generate_series(1, 6000);

-- 3. 모든 테스트 사용자에게 USER 권한 부여
INSERT INTO member_role (member_id, role_id)
SELECT 
    m.id,
    r.id
FROM member m, role r
WHERE m.provider = 'test' 
  AND r.name = 'USER';

-- 4. 생성 결과 확인
SELECT 
    COUNT(*) as total_test_users,
    MIN(created_at) as earliest_created,
    MAX(created_at) as latest_created
FROM member 
WHERE provider = 'test';
```

### 2.2 시나리오 B용 기존 리캡 데이터 생성

#### 2.2.1 테스트 영상 데이터 생성

```sql
-- 파일명: create_test_videos.sql
-- 각 테스트 사용자마다 1-3개의 과거 영상 생성

INSERT INTO video (
    id,
    storyboard_id,
    member_id,
    video_url,
    thumbnail_url,
    title,
    created_at
)
SELECT 
    uuid_generate_v4(),                           -- video 테이블은 uuid_generate_v4() 사용
    -- 기존 스토리보드 중 랜덤 선택 (실제 존재하는 UUID로 교체 필요)
    (ARRAY[
        'E5E9B7DC-EFA4-43F9-B428-03769AABDAFC'::uuid,  -- 여행
        'C81D9417-5797-4B11-A8EA-C161CACFE9D1'::uuid,  -- 회고
        'CFF1C432-B6AC-4B10-89B7-3C9BE91A6699'::uuid,  -- 짝사랑
        '8c2746c4-4613-47f8-8799-235fec7f359d'::uuid   -- 자기소개
    ])[ceil(random() * 4)::int],
    m.id,
    'https://test-bucket.s3.amazonaws.com/videos/' || m.id || '/' || video_num || '.mp4',
    'https://test-bucket.s3.amazonaws.com/thumbnails/' || m.id || '/' || video_num || '.jpg',
    'Test Video ' || video_num,
    CURRENT_TIMESTAMP - INTERVAL '1 day' * (random() * 60)  -- 지난 2달 내
FROM 
    member m,
    generate_series(1, 1 + floor(random() * 3)::int) as video_num  -- 1-3개 랜덤
WHERE m.provider = 'test';

-- 생성 결과 확인
SELECT 
    COUNT(*) as total_test_videos,
    COUNT(DISTINCT member_id) as users_with_videos,
    AVG(video_count) as avg_videos_per_user
FROM (
    SELECT member_id, COUNT(*) as video_count
    FROM video v
    JOIN member m ON v.member_id = m.id
    WHERE m.provider = 'test'
    GROUP BY member_id
) video_stats;
```

#### 2.2.2 리캡 예약 및 결과 데이터 생성

```sql
-- 파일명: create_test_recap_data.sql
-- interview_audio_recording 테이블에 오디오 녹음 데이터 생성

-- 1. 각 비디오에 대해 하나의 오디오 녹음 생성
WITH video_audio_pairs AS (
    SELECT 
        uuid_generate_v4() as audio_id,
        v.id as video_id,
        v.storyboard_id,
        v.member_id,
        v.created_at
    FROM video v
    WHERE EXISTS (SELECT 1 FROM member m WHERE m.id = v.member_id AND m.provider = 'test')
)
INSERT INTO interview_audio_recording (
    id,
    storyboard_id,
    member_id,
    video_url,
    running_time,
    created_at
)
SELECT 
    audio_id,
    storyboard_id,
    member_id,
    'https://test-bucket.s3.amazonaws.com/audio/' || video_id || '.mp3',
    420 + floor(random() * 180)::int,  -- 7-10분 (420-600초)
    created_at + INTERVAL '5 minutes'
FROM video_audio_pairs;

-- 2. recap_result 테이블에 리캡 결과 생성
WITH audio_result_pairs AS (
    SELECT 
        iar.id as audio_id,
        gen_random_uuid() as result_id,
        iar.created_at + INTERVAL '2 hours' as result_created_at
    FROM interview_audio_recording iar
    WHERE EXISTS (
        SELECT 1 FROM member m 
        WHERE m.id = iar.member_id AND m.provider = 'test'
    )
)
INSERT INTO recap_result (id, created_at)
SELECT result_id, result_created_at
FROM audio_result_pairs;

-- 3. recap_reservation 테이블에 완료된 리캡 예약 생성
WITH reservation_data AS (
    SELECT 
        uuid_generate_v4() as reservation_id,
        v.member_id,
        v.id as video_id,
        iar.id as audio_id,
        rr.id as result_id,
        iar.created_at as scheduled_at,
        iar.created_at - INTERVAL '30 minutes' as reservation_created_at
    FROM video v
    JOIN interview_audio_recording iar 
        ON iar.member_id = v.member_id 
        AND iar.storyboard_id = v.storyboard_id
    JOIN recap_result rr 
        ON rr.created_at = iar.created_at + INTERVAL '2 hours'
    WHERE EXISTS (SELECT 1 FROM member m WHERE m.id = v.member_id AND m.provider = 'test')
)
INSERT INTO recap_reservation (
    id,
    member_id,
    video_id,
    interview_audio_recording_id,
    recap_result_id,
    scheduled_at,
    created_at
)
SELECT 
    reservation_id,
    member_id,
    video_id,
    audio_id,
    result_id,
    scheduled_at,
    reservation_created_at
FROM reservation_data;

-- 4. recap_answer_summary 테이블에 리캡 답변 요약 생성 (scene별)
INSERT INTO recap_answer_summary (
    id,
    recap_result_id,
    scene_id,
    summary,
    scene_order,
    created_at
)
SELECT 
    gen_random_uuid(),
    rr.recap_result_id,
    s.id,
    '테스트용 Scene ' || ROW_NUMBER() OVER (PARTITION BY rr.recap_result_id ORDER BY s.id) || ' 답변 요약입니다.',
    ROW_NUMBER() OVER (PARTITION BY rr.recap_result_id ORDER BY s.id),
    rr.created_at
FROM recap_reservation rr
JOIN video v ON v.id = rr.video_id
JOIN scene s ON s.storyboard_id = v.storyboard_id
JOIN recap_result result ON result.id = rr.recap_result_id
WHERE EXISTS (
    SELECT 1 FROM member m 
    WHERE m.id = rr.member_id AND m.provider = 'test'
);

-- 생성 결과 확인
SELECT 
    'audio_recordings' as table_name,
    COUNT(*) as record_count
FROM interview_audio_recording iar
JOIN member m ON iar.member_id = m.id
WHERE m.provider = 'test'

UNION ALL

SELECT 
    'recap_reservations' as table_name,
    COUNT(*) as record_count
FROM recap_reservation rr
JOIN member m ON rr.member_id = m.id
WHERE m.provider = 'test'

UNION ALL

SELECT 
    'recap_results' as table_name,
    COUNT(*) as record_count
FROM recap_result rr
WHERE EXISTS (
    SELECT 1 FROM recap_reservation res
    JOIN member m ON res.member_id = m.id
    WHERE res.recap_result_id = rr.id AND m.provider = 'test'
);
```

---

## 3. nGrinder AuthHelper 구체적 구현

### 3.1 AuthHelper 클래스

```groovy
// src/test/ngrinder/lib/AuthHelper.groovy
class AuthHelper {
    
    /**
     * 테스트 사용자 인증 처리
     * @param request HTTPRequest 객체
     * @param userId 사용자 ID (1-6000)
     * @return JWT 토큰 문자열
     */
    static String authenticateTestUser(HTTPRequest request, int userId) {
        def baseUrl = TestConfig.API_BASE_URL
        def authCode = "test_user_${userId}"
        
        try {
            // OAuth 콜백 시뮬레이션
            def response = request.GET("${baseUrl}/api/v0/auth/callback/test?code=${authCode}")
            
            if (response.statusCode != 302) {
                throw new RuntimeException("Authentication failed with status: ${response.statusCode}")
            }
            
            // 리다이렉트 URL에서 JWT 토큰 추출
            def location = response.getHeader("Location")
            def tokenMatch = location =~ /jwtToken=([^&]+)/
            
            if (tokenMatch) {
                return URLDecoder.decode(tokenMatch[0][1], "UTF-8")
            } else {
                throw new RuntimeException("Failed to extract JWT token from: ${location}")
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Authentication error for user ${userId}: ${e.message}", e)
        }
    }
    
    /**
     * JWT 토큰을 포함한 HTTP 헤더 생성
     * @param jwtToken JWT 토큰
     * @return 인증 헤더 맵
     */
    static Map<String, String> getAuthHeaders(String jwtToken) {
        return [
            "Authorization": "Bearer ${jwtToken}",
            "Content-Type": "application/json",
            "User-Agent": "nGrinder/3.5.8"
        ]
    }
    
    /**
     * 토큰 만료 여부 확인
     * @param response HTTP 응답
     * @return 토큰 만료 여부
     */
    static boolean isTokenExpired(HTTPResponse response) {
        return response.statusCode == 401 || 
               (response.statusCode == 403 && response.text?.contains("token"))
    }
    
    /**
     * 토큰 만료 시 재인증 수행
     * @param request HTTPRequest 객체
     * @param userId 사용자 ID
     * @return 새로운 JWT 토큰
     */
    static String refreshToken(HTTPRequest request, int userId) {
        // 토큰 만료 시 재인증
        return authenticateTestUser(request, userId)
    }
    
    /**
     * 인증이 필요한 API 호출 래퍼
     * @param request HTTPRequest 객체
     * @param userId 사용자 ID  
     * @param method HTTP 메서드
     * @param url 요청 URL
     * @param body 요청 본문 (선택사항)
     * @return HTTP 응답
     */
    static HTTPResponse callAuthenticatedAPI(HTTPRequest request, int userId, 
                                           String method, String url, String body = null) {
        static Map<Integer, String> tokenCache = [:]
        
        // 캐시된 토큰 사용 또는 새로 발급
        String token = tokenCache.get(userId)
        if (!token) {
            token = authenticateTestUser(request, userId)
            tokenCache.put(userId, token)
        }
        
        def headers = getAuthHeaders(token)
        HTTPResponse response
        
        // HTTP 메서드에 따른 요청 실행
        switch (method.toUpperCase()) {
            case "GET":
                response = request.GET(url, headers)
                break
            case "POST":
                response = request.POST(url, body, headers)
                break
            case "PUT":
                response = request.PUT(url, body, headers)
                break
            case "DELETE":
                response = request.DELETE(url, headers)
                break
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: ${method}")
        }
        
        // 토큰 만료 시 재시도
        if (isTokenExpired(response)) {
            token = refreshToken(request, userId)
            tokenCache.put(userId, token)
            headers = getAuthHeaders(token)
            
            // 재요청
            switch (method.toUpperCase()) {
                case "GET":
                    response = request.GET(url, headers)
                    break
                case "POST":
                    response = request.POST(url, body, headers)
                    break
                case "PUT":
                    response = request.PUT(url, body, headers)
                    break
                case "DELETE":
                    response = request.DELETE(url, headers)
                    break
            }
        }
        
        return response
    }
}
```

### 3.2 UserScenario에서 사용 예시

```groovy
// UserScenarioA.groovy에서 사용법
@Test
public void testScenarioA() {
    // 1. 사용자 ID 할당 (1-6000 범위)
    def userId = ThreadLocalRandom.current().nextInt(1, 6001)
    
    try {
        // 2. 토픽 목록 조회
        def topicResponse = AuthHelper.callAuthenticatedAPI(
            request, userId, "GET", "${TestConfig.API_BASE_URL}/api/v0/topic/list"
        )
        
        grinder.statistics.forLastTest.success = (topicResponse.statusCode == 200)
        
        // 3. 내 영상 목록 조회
        Thread.sleep(500) // 0.5초 대기
        
        def videoResponse = AuthHelper.callAuthenticatedAPI(
            request, userId, "GET", "${TestConfig.API_BASE_URL}/api/v0/archive/videos/my"
        )
        
        grinder.statistics.forLastTest.success = (videoResponse.statusCode == 200)
        
        // 4. 스토리보드 선택 및 미리보기 조회 (4회)
        def storyboardId = TestDataProvider.getStoryboardByUserId(userId)
        
        for (int i = 0; i < 4; i++) {
            Thread.sleep(2000) // 2초 대기
            
            def previewResponse = AuthHelper.callAuthenticatedAPI(
                request, userId, "GET", 
                "${TestConfig.API_BASE_URL}/api/v0/storyboard/${storyboardId}/preview"
            )
            
            grinder.statistics.forLastTest.success = (previewResponse.statusCode == 200)
        }
        
        // ... 추가 API 호출 로직
        
    } catch (Exception e) {
        grinder.logger.error("Scenario A failed for user ${userId}: ${e.message}")
        grinder.statistics.forLastTest.success = false
    }
}
```

---

## 4. 환경 설정 및 보안

### 4.1 Application Properties

```properties
# application-loadtest.properties
# 테스트 인증 활성화
test.auth.enabled=true

# 프론트엔드 콜백 URL (테스트용)
security.frontend.callback-url=http://localhost:3000/auth/callback

# JWT 설정 (확인 필요한 값들)
security.jwt.expiration=3600000
security.jwt.secret=${JWT_SECRET:test-secret-key-for-load-testing}

# DB Connection Pool 최적화 (HikariCP)
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=20
spring.datasource.hikari.connection-timeout=10000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000

# JPA/Hibernate 배치 처리 최적화
spring.jpa.hibernate.jdbc.batch_size=25
spring.jpa.hibernate.order_inserts=true
spring.jpa.hibernate.order_updates=true
spring.jpa.hibernate.batch_versioned_data=true

# Tomcat 최적화
server.tomcat.threads.max=100
server.tomcat.threads.min-spare=50
server.tomcat.max-connections=200
server.tomcat.accept-count=100

# 로깅 설정 (테스트 시 디버깅용)
logging.level.com.orv.api=INFO
logging.level.com.orv.api.domain.auth=DEBUG
logging.level.com.orv.api.domain.reservation=DEBUG
logging.level.org.springframework.web=WARN
logging.level.com.zaxxer.hikari=DEBUG

# 테스트 환경 최적화
spring.jpa.show-sql=false
spring.jpa.hibernate.ddl-auto=none
spring.cache.type=simple

# 파일 업로드 설정
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

### 4.2 보안 설정

```java
// TestSecurityConfig.java (테스트 전용 설정)
@Configuration
@Profile({"loadtest", "test"})
@ConditionalOnProperty(name = "test.auth.enabled", havingValue = "true")
public class TestSecurityConfig {
    
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        // 테스트 환경에서는 단순한 인코더 사용 (성능 최적화)
        return NoOpPasswordEncoder.getInstance();
    }
    
    @Bean
    public CorsConfigurationSource testCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### 4.3 안전장치

#### A. 환경 분리 검증
```java
@Component
@ConditionalOnProperty(name = "test.auth.enabled", havingValue = "true")
public class TestEnvironmentValidator implements ApplicationRunner {
    
    @Value("${spring.profiles.active:}")
    private String activeProfiles;
    
    @Override
    public void run(ApplicationArguments args) {
        List<String> profiles = Arrays.asList(activeProfiles.split(","));
        
        if (profiles.contains("prod") || profiles.contains("production")) {
            throw new IllegalStateException(
                "테스트 인증이 프로덕션 환경에서 활성화될 수 없습니다!"
            );
        }
        
        logger.info("테스트 환경 검증 완료: {}", profiles);
    }
}
```

#### B. 데이터 격리 확인
```sql
-- 테스트 데이터 격리 검증 쿼리
SELECT 
    provider,
    COUNT(*) as user_count,
    MIN(created_at) as earliest,
    MAX(created_at) as latest
FROM member 
GROUP BY provider
ORDER BY provider;

-- 결과 예시:
-- provider | user_count | earliest            | latest
-- google   |       150  | 2024-01-01 10:00:00 | 2025-07-24 15:30:00
-- kakao    |        89  | 2024-02-15 09:15:00 | 2025-07-23 18:45:00
-- test     |      6000  | 2024-07-25 14:00:00 | 2024-07-25 14:00:01
```

---

## 5. 테스트 데이터 정리 스크립트

### 5.1 외래키 제약사항 고려한 순서별 삭제

```sql
-- 파일명: cleanup_test_data.sql
-- 테스트 데이터 정리 스크립트 (의존성 순서 고려)

BEGIN;

-- 1. recap_answer_summary 삭제
DELETE FROM recap_answer_summary WHERE recap_result_id IN (
    SELECT DISTINCT rr.recap_result_id 
    FROM recap_reservation rr 
    JOIN member m ON m.id = rr.member_id 
    WHERE m.provider = 'test'
);

-- 2. recap_reservation 삭제
DELETE FROM recap_reservation WHERE member_id IN (
    SELECT id FROM member WHERE provider = 'test'
);

-- 3. recap_result 삭제 (고아 레코드)
DELETE FROM recap_result WHERE id NOT IN (
    SELECT DISTINCT recap_result_id 
    FROM recap_reservation 
    WHERE recap_result_id IS NOT NULL
);

-- 4. interview_audio_recording 삭제
DELETE FROM interview_audio_recording WHERE member_id IN (
    SELECT id FROM member WHERE provider = 'test'
);

-- 5. video 삭제
DELETE FROM video WHERE member_id IN (
    SELECT id FROM member WHERE provider = 'test'
);

-- 6. member_role 삭제
DELETE FROM member_role WHERE member_id IN (
    SELECT id FROM member WHERE provider = 'test'
);

-- 7. member 삭제
DELETE FROM member WHERE provider = 'test';

-- 8. 삭제 결과 확인
SELECT 
    'Cleanup completed' as status,
    COUNT(*) as remaining_test_users
FROM member 
WHERE provider = 'test';

-- 9. 통계 정보 업데이트
ANALYZE member;
ANALYZE video;
ANALYZE recap_reservation;
ANALYZE interview_audio_recording;

COMMIT;
```

### 5.2 정리 스크립트 자동화

#### A. 정리 스크립트 실행기
```bash
#!/bin/bash
# cleanup_test_data.sh
set -e

echo "=== 테스트 데이터 정리 시작 ===" 
echo "시작 시간: $(date)"

# 환경 변수 확인
if [ -z "$DB_HOST" ] || [ -z "$DB_USER" ] || [ -z "$DB_NAME" ]; then
    echo "❌ 필수 환경 변수가 설정되지 않았습니다."
    echo "DB_HOST, DB_USER, DB_NAME을 설정해주세요."
    exit 1
fi

# 사용자 확인
echo "다음 데이터베이스의 테스트 데이터를 정리합니다:"
echo "Host: $DB_HOST"
echo "Database: $DB_NAME"
echo "User: $DB_USER"
echo ""
read -p "계속하시겠습니까? (y/N): " confirm

if [[ $confirm != [yY] ]]; then
    echo "작업이 취소되었습니다."
    exit 0
fi

# 정리 전 백업 (선택사항)
if [[ "${BACKUP_BEFORE_CLEANUP:-false}" == "true" ]]; then
    echo "📦 정리 전 테스트 데이터 백업 중..."
    pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME \
        --table=member --table=video --table=recap_reservation \
        --where="provider='test'" \
        -f "test_data_backup_$(date +%Y%m%d_%H%M%S).sql"
fi

# 정리 스크립트 실행
echo "🧹 테스트 데이터 정리 중..."
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -f cleanup_test_data.sql

# 정리 결과 확인
echo "✅ 정리 완료!"
echo "완료 시간: $(date)"

# 정리 후 통계
echo ""
echo "=== 정리 후 DB 상태 ==="
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "
SELECT 
    'member' as table_name,
    COUNT(*) as total_records,
    COUNT(CASE WHEN provider = 'test' THEN 1 END) as test_records
FROM member
UNION ALL
SELECT 
    'video' as table_name,
    COUNT(*) as total_records,
    COUNT(CASE WHEN EXISTS (
        SELECT 1 FROM member m WHERE m.id = video.member_id AND m.provider = 'test'
    ) THEN 1 END) as test_records
FROM video;
"

echo "=== 테스트 데이터 정리 완료 ==="
```

#### B. 환경 변수 설정 예시
```bash
# load_test_env.sh
#!/bin/bash

# 데이터베이스 연결 정보
export DB_HOST="your-rds-endpoint.amazonaws.com"
export DB_USER="your_username"
export DB_NAME="your_database"
export PGPASSWORD="your_password"  # 또는 .pgpass 파일 사용

# 선택적 설정
export BACKUP_BEFORE_CLEANUP=true

# AWS 설정 (S3 정리용)
export AWS_REGION="ap-northeast-2"
export TEST_BUCKET="your-test-bucket"

echo "환경 변수 설정 완료"
echo "DB_HOST: $DB_HOST"
echo "DB_NAME: $DB_NAME"
echo "DB_USER: $DB_USER"
echo "사용법: source load_test_env.sh"
```

### 5.3 S3 테스트 파일 정리

```bash
#!/bin/bash
# cleanup_s3_test_files.sh

echo "=== S3 테스트 파일 정리 시작 ==="

if [ -z "$TEST_BUCKET" ]; then
    echo "❌ TEST_BUCKET 환경 변수가 설정되지 않았습니다."
    exit 1
fi

# 테스트 파일 경로들
TEST_PATHS=(
    "videos/test/"
    "audio/test/"
    "thumbnails/test/"
    "temp/"
)

for path in "${TEST_PATHS[@]}"; do
    echo "🧹 정리 중: s3://$TEST_BUCKET/$path"
    aws s3 rm "s3://$TEST_BUCKET/$path" --recursive --quiet
    
    if [ $? -eq 0 ]; then
        echo "✅ 완료: $path"
    else
        echo "❌ 실패: $path"
    fi
done

echo "=== S3 테스트 파일 정리 완료 ==="
```

---

## 6. 실행 가이드

### 6.1 테스트 데이터 생성 순서

```bash
# 1. 환경 설정
source load_test_env.sh

# 2. 테스트 사용자 생성
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -f create_test_users.sql

# 3. 테스트 영상 데이터 생성  
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -f create_test_videos.sql

# 4. 리캡 데이터 생성
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -f create_test_recap_data.sql

# 5. 데이터 생성 확인
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "
SELECT 
    'test_users' as data_type,
    COUNT(*) as count
FROM member WHERE provider = 'test'
UNION ALL
SELECT 
    'test_videos' as data_type,
    COUNT(*) as count
FROM video v JOIN member m ON v.member_id = m.id WHERE m.provider = 'test'
UNION ALL
SELECT 
    'test_recap_reservations' as data_type,
    COUNT(*) as count
FROM recap_reservation rr JOIN member m ON rr.member_id = m.id WHERE m.provider = 'test';
"
```

### 6.2 주의사항

#### A. 실행 전 확인사항
- [ ] **프로덕션 환경 접근 금지**: 반드시 테스트/개발 환경에서만 실행
- [ ] **스키마 호환성**: 실제 DB 스키마와 스크립트 호환성 확인
- [ ] **권한 설정**: 데이터베이스 접근 권한 및 S3 권한 확인
- [ ] **백업**: 중요한 경우 기존 데이터 백업 수행

#### B. 실행 후 검증사항
1. **데이터 정합성 검증**
   ```sql
   -- 외래키 제약 위반 확인
   SELECT COUNT(*) as orphaned_videos 
   FROM video v 
   LEFT JOIN member m ON v.member_id = m.id 
   WHERE m.id IS NULL;
   ```

2. **테스트 인증 동작 확인**
   ```bash
   curl -X GET "http://localhost:8080/api/v0/auth/callback/test?code=test_user_1" \
        -H "Accept: application/json"
   ```

3. **부하테스트 준비 상태 확인**
   - [ ] 6,000명 테스트 사용자 생성 완료
   - [ ] 기존 리캡 데이터 존재 (시나리오 B용)
   - [ ] TestAuthService 정상 동작
   - [ ] DB Connection Pool 설정 적용

## 📋 관련 문서

- **메인 문서**: [README.md](README.md)에서 전체 문서 구조 확인
- **구현 계획**: [구현 계획](05-implementation-plan.md)에서 Phase별 구현 일정 확인
- **프로젝트 구조**: [프로젝트 구조](04-project-structure.md)에서 검증 결과 및 필수 보완사항 확인

---

**[← 이전: 모니터링 및 분석](08-monitoring.md)** | **[메인으로 돌아가기](README.md)**
