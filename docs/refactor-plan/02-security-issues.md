# 보안 이슈 및 개선방안

## 🚨 치명적인 보안 취약점

### 1. CORS 설정 문제

#### 현재 상태
```java
// SecurityConfig.java
configuration.setAllowedOriginPatterns(List.of("*")); // 모든 origin 허용!
configuration.setAllowCredentials(false);
```

#### 문제점
- 모든 도메인에서 API 호출 가능
- CSRF 공격에 취약
- 민감한 데이터 유출 위험

#### 개선 방안
```java
@Configuration
public class CorsConfig {
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins); // 특정 도메인만 허용
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### 2. 파일 업로드 검증 부재

#### 현재 상태
```java
// ArchiveController.java
@PostMapping("/recorded-video")
public ApiResponse uploadRecordedVideo(@RequestParam("video") MultipartFile video, ...) {
    // 파일 타입, 크기 검증 없음!
    Optional<String> videoId = videoRepository.save(
        video.getInputStream(),
        new VideoMetadata(...)
    );
}
```

#### 문제점
- 악성 파일 업로드 가능
- 서버 리소스 고갈 공격 가능
- 스토리지 비용 폭증 위험

#### 개선 방안
```java
@Component
public class FileUploadValidator {
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
        "video/mp4", "video/mpeg", "video/quicktime"
    );
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp"
    );
    
    public void validateVideoFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("파일이 비어있습니다");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("파일 크기가 100MB를 초과합니다");
        }
        
        String contentType = file.getContentType();
        if (!ALLOWED_VIDEO_TYPES.contains(contentType)) {
            throw new InvalidFileException("지원하지 않는 비디오 형식입니다");
        }
        
        // 파일 시그니처 검증 (Magic Number)
        validateFileSignature(file);
    }
    
    private void validateFileSignature(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            is.read(header);
            
            // MP4 파일 시그니처 검증
            if (!isValidMp4Signature(header)) {
                throw new InvalidFileException("유효하지 않은 파일 형식입니다");
            }
        } catch (IOException e) {
            throw new FileProcessingException("파일 검증 중 오류가 발생했습니다");
        }
    }
}
```

### 3. 시크릿 관리 문제

#### 현재 상태
```java
// application.properties
jwt.secret=${JWT_SECRET}  // 환경변수로만 관리

// S3Config.java
@Value("${cloud.aws.cloudfront.domain}")
private String cloudfrontDomain; // 하드코딩 위험
```

#### 문제점
- 환경변수 유출 시 전체 시스템 위험
- 키 로테이션 어려움
- 감사(Audit) 추적 불가

#### 개선 방안
```java
@Configuration
public class SecretsConfig {
    private final AWSSecretsManager secretsManager;
    
    @Bean
    public JwtProperties jwtProperties() {
        String secretName = "orv-api/jwt";
        GetSecretValueRequest request = new GetSecretValueRequest()
            .withSecretId(secretName);
        
        GetSecretValueResult result = secretsManager.getSecretValue(request);
        Map<String, String> secrets = parseSecretJson(result.getSecretString());
        
        return JwtProperties.builder()
            .secret(secrets.get("secret"))
            .expiration(Long.parseLong(secrets.get("expiration")))
            .build();
    }
    
    @Bean
    @RefreshScope // 동적 갱신 지원
    public AwsProperties awsProperties() {
        // AWS Secrets Manager에서 CloudFront 도메인 등 조회
    }
}
```

### 4. 에러 처리로 인한 정보 노출

#### 현재 상태
```java
} catch (Exception e) {
    e.printStackTrace(); // 스택 트레이스 콘솔 출력!
    return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
}
```

#### 문제점
- 내부 구조 노출
- 데이터베이스 스키마 유출 가능
- 공격 벡터 제공

#### 개선 방안
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse> handleBusinessException(BusinessException e) {
        log.warn("Business exception occurred: {}", e.getMessage());
        return ResponseEntity
            .status(e.getHttpStatus())
            .body(ApiResponse.fail(e.getErrorCode(), e.getHttpStatus()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleUnexpectedException(Exception e) {
        // 에러 ID 생성하여 추적 가능하게 함
        String errorId = UUID.randomUUID().toString();
        log.error("Unexpected error [{}]", errorId, e);
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.fail(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "서버 오류가 발생했습니다. 에러 ID: " + errorId
            ));
    }
}
```

## 🔒 추가 보안 강화 방안

### 1. API Rate Limiting
```java
@Configuration
public class RateLimitConfig {
    @Bean
    public RateLimiter rateLimiter() {
        return RateLimiter.of("api", RateLimiterConfig.custom()
            .limitForPeriod(100)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofMillis(100))
            .build());
    }
}
```

### 2. SQL Injection 방지
```java
// 파라미터 바인딩 사용 강제
public Optional<Member> findByNickname(String nickname) {
    String sql = "SELECT * FROM member WHERE nickname = ?";
    // PreparedStatement 자동 사용
    return jdbcTemplate.queryForObject(sql, 
        new Object[]{nickname}, // 파라미터 바인딩
        new BeanPropertyRowMapper<>(Member.class));
}
```

### 3. 입력 검증 강화
```java
@RestController
@Validated // 검증 활성화
public class MemberController {
    @PostMapping("/join")
    public ApiResponse join(@Valid @RequestBody JoinRequest request) {
        // @Valid로 자동 검증
    }
}

public class JoinRequest {
    @NotBlank
    @Pattern(regexp = "^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]{1,8}$")
    private String nickname;
    
    @NotNull
    @Past
    private LocalDate birthday;
    
    @Pattern(regexp = "^01[0-9]{8,9}$")
    private String phoneNumber;
}
```

### 4. 보안 헤더 설정
```java
@Configuration
public class SecurityHeaderConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .contentSecurityPolicy("default-src 'self'")
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
            .xssProtection(xss -> xss.block(true))
            .contentTypeOptions(Customizer.withDefaults())
        );
        return http.build();
    }
}
```

## 📋 Action Items

### 즉시 조치 (1주 이내)
1. [ ] CORS 설정을 특정 도메인으로 제한
2. [ ] 파일 업로드 검증 로직 추가
3. [ ] 에러 핸들러 구현 및 스택 트레이스 제거
4. [ ] SQL Injection 취약점 점검

### 단기 조치 (2주 이내)
1. [ ] AWS Secrets Manager 도입
2. [ ] Rate Limiting 구현
3. [ ] 입력 검증 강화 (@Valid, @Pattern)
4. [ ] 보안 헤더 설정

### 중기 조치 (1개월 이내)
1. [ ] 보안 감사(Audit) 로깅 구현
2. [ ] 침입 탐지 시스템 연동
3. [ ] 정기적인 보안 스캔 자동화
4. [ ] OWASP Top 10 체크리스트 검증

## 🔍 보안 테스트 체크리스트

- [ ] Burp Suite를 이용한 취약점 스캔
- [ ] OWASP ZAP 자동화 테스트
- [ ] SQL Injection 테스트
- [ ] XSS 공격 테스트
- [ ] CSRF 토큰 검증
- [ ] 파일 업로드 악용 테스트
- [ ] API Rate Limit 테스트

---

← 이전: [현재 상태 분석](./01-current-analysis.md) | 다음: [아키텍처 개선사항](./03-architecture-improvements.md) →
