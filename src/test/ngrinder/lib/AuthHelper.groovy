package lib

import static net.grinder.script.Grinder.grinder
import net.grinder.plugin.http.HTTPRequest
import net.grinder.plugin.http.HTTPResponse

/**
 * JWT 토큰 인증 헬퍼 클래스
 * Phase 1 Day 2에서 구현된 인증 관리 클래스
 */
class AuthHelper {
    private String accessToken
    private long tokenExpireTime
    private int userId
    private HTTPRequest request
    private MetricsCollector metricsCollector
    
    /**
     * AuthHelper 생성자
     * @param request nGrinder HTTPRequest 인스턴스
     * @param metricsCollector 메트릭 수집기
     */
    AuthHelper(HTTPRequest request, MetricsCollector metricsCollector) {
        this.request = request
        this.metricsCollector = metricsCollector
    }
    
    /**
     * 테스트 사용자 인증 및 JWT 토큰 획득
     * Phase 0에서 구현한 TestAuthService를 활용
     * agent_process_thread 형식으로 인증 코드 생성
     * 
     * @param userId 테스트 사용자 ID (1-6000)
     * @return JWT 액세스 토큰
     */
    String authenticate(int userId) {
        this.userId = userId
        
        try {
            // nGrinder 런타임 정보 가져오기 (분산 환경 지원)
            def agentNumber = grinder.agentNumber ?: 0
            def processNumber = grinder.processNumber ?: 0
            
            // agent_process_thread 형식으로 인증 코드 생성
            // TestAuthService가 요구하는 형식: test_user_0_0_1
            String authCode = "${TestConfig.TEST_USER_PREFIX}${agentNumber}_${processNumber}_${userId}"
            String authUrl = "${ApiEndpoints.AUTH_TEST_CALLBACK}?code=${authCode}"
            
            grinder.logger.info("Authenticating with code: ${authCode} (agent=${agentNumber}, process=${processNumber}, thread=${userId})")
            
            // 인증 요청 실행
            long startTime = System.currentTimeMillis()
            HTTPResponse response = request.GET(authUrl)
            long responseTime = System.currentTimeMillis() - startTime
            
            // 메트릭 수집
            metricsCollector?.recordResponse("AUTH_TEST_CALLBACK", responseTime, response.statusCode)
            
            if (response.statusCode == 200) {
                // JWT 토큰 추출 (응답 body에서 access_token 파싱)
                String responseBody = response.getText()
                this.accessToken = extractTokenFromResponse(responseBody)
                
                // 토큰 만료 시간 계산 (기본 12시간, 720분)
                this.tokenExpireTime = System.currentTimeMillis() + (720 * 60 * 1000)
                
                grinder.logger.info("User ${userId} authenticated successfully")
                return this.accessToken
                
            } else {
                String errorMsg = "Authentication failed for user ${userId}: HTTP ${response.statusCode}"
                grinder.logger.error(errorMsg)
                metricsCollector?.logError("AUTH_TEST_CALLBACK", errorMsg)
                throw new RuntimeException(errorMsg)
            }
            
        } catch (Exception e) {
            String errorMsg = "Authentication error for user ${userId}: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector?.logError("AUTH_TEST_CALLBACK", errorMsg)
            throw e
        }
    }
    
    /**
     * 토큰 만료 여부 확인
     * JWT_EXPIRATION_BUFFER(60초) 여유시간 고려
     * 
     * @return true if token is expired or about to expire
     */
    boolean isTokenExpired() {
        if (tokenExpireTime == 0) {
            return true  // 토큰이 설정되지 않음
        }
        
        long currentTime = System.currentTimeMillis()
        long bufferTime = TestConfig.JWT_EXPIRATION_BUFFER * 1000  // 60초를 밀리초로
        
        return (currentTime + bufferTime) >= tokenExpireTime
    }
    
    /**
     * 토큰 재인증 (토큰 만료 시 자동 호출)
     * 
     * @return 새로운 JWT 액세스 토큰
     */
    String refreshToken() {
        grinder.logger.info("Refreshing token for user ${userId}")
        return authenticate(userId)
    }
    
    /**
     * Authorization 헤더 생성
     * 토큰 만료 검사 및 자동 재인증 포함
     * 
     * @return HTTP 헤더 맵
     */
    Map<String, String> getAuthHeaders() {
        // 토큰 만료 검사 및 자동 재인증
        if (isTokenExpired()) {
            refreshToken()
        }
        
        return [
            "Authorization": "Bearer ${accessToken}",
            "Content-Type": "application/json"
        ]
    }
    
    /**
     * multipart 요청용 Authorization 헤더 생성
     * Content-Type을 multipart/form-data로 설정하지 않음 (자동 설정)
     * 
     * @return HTTP 헤더 맵
     */
    Map<String, String> getAuthHeadersForMultipart() {
        // 토큰 만료 검사 및 자동 재인증
        if (isTokenExpired()) {
            refreshToken()
        }
        
        return [
            "Authorization": "Bearer ${accessToken}"
            // Content-Type은 multipart 요청 시 자동 설정됨
        ]
    }
    
    /**
     * 인증된 GET 요청 실행
     * 
     * @param url 요청 URL
     * @param apiName 메트릭 수집용 API 이름
     * @return HTTPResponse
     */
    HTTPResponse authenticatedGet(String url, String apiName) {
        Map<String, String> headers = getAuthHeaders()
        
        long startTime = System.currentTimeMillis()
        HTTPResponse response = request.GET(url, headers)
        long responseTime = System.currentTimeMillis() - startTime
        
        metricsCollector?.recordResponse(apiName, responseTime, response.statusCode)
        
        if (response.statusCode >= 400) {
            String errorMsg = "${apiName} failed: HTTP ${response.statusCode}"
            grinder.logger.error(errorMsg)
            metricsCollector?.logError(apiName, errorMsg)
        }
        
        return response
    }
    
    /**
     * 인증된 POST 요청 실행
     * 
     * @param url 요청 URL
     * @param body 요청 바디
     * @param apiName 메트릭 수집용 API 이름
     * @return HTTPResponse
     */
    HTTPResponse authenticatedPost(String url, String body, String apiName) {
        Map<String, String> headers = getAuthHeaders()
        
        long startTime = System.currentTimeMillis()
        HTTPResponse response = request.POST(url, body, headers)
        long responseTime = System.currentTimeMillis() - startTime
        
        metricsCollector?.recordResponse(apiName, responseTime, response.statusCode)
        
        if (response.statusCode >= 400) {
            String errorMsg = "${apiName} failed: HTTP ${response.statusCode}"
            grinder.logger.error(errorMsg)
            metricsCollector?.logError(apiName, errorMsg)
        }
        
        return response
    }
    
    /**
     * 응답에서 JWT 토큰 추출
     * JSON 응답 파싱하여 access_token 필드 추출
     * 
     * @param responseBody JSON 응답 문자열
     * @return JWT 액세스 토큰
     */
    private String extractTokenFromResponse(String responseBody) {
        try {
            // 간단한 JSON 파싱 (정규표현식 사용)
            def tokenPattern = /"access_token"\s*:\s*"([^"]+)"/
            def matcher = responseBody =~ tokenPattern
            
            if (matcher.find()) {
                return matcher.group(1)
            } else {
                throw new RuntimeException("access_token not found in response")
            }
            
        } catch (Exception e) {
            grinder.logger.error("Token extraction failed: ${e.message}")
            throw new RuntimeException("Failed to extract JWT token: ${e.message}")
        }
    }
    
    /**
     * 현재 사용자 ID 반환
     * 
     * @return 테스트 사용자 ID
     */
    int getUserId() {
        return userId
    }
    
    /**
     * 현재 액세스 토큰 반환 (디버깅용)
     * 
     * @return JWT 액세스 토큰
     */
    String getAccessToken() {
        return accessToken
    }
}
