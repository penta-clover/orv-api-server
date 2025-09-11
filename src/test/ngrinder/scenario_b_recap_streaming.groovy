import static net.grinder.script.Grinder.grinder
import net.grinder.plugin.http.HTTPRequest
import net.grinder.plugin.http.HTTPResponse
import net.grinder.script.GTest
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import lib.TestConfig
import lib.ApiEndpoints
import lib.AuthHelper
import lib.MediaHelper
import lib.MetricsCollector
import lib.TestDataProvider

/**
 * ORV API Server 부하테스트 시나리오 B: 리캡 조회 및 오디오 스트리밍
 * 
 * 테스트 플로우:
 * 1. TestAuthService를 통한 인증 (/auth/callback/test?code=test_user_1)
 * 2. 내 리캡 목록 조회
 * 3. 기존 리캡 결과 선택 (Phase 0에서 생성한 데이터 활용)
 * 4. 리캡 결과 상세 조회
 * 5. 리캡 오디오 URL 조회
 * 6. 오디오 스트리밍 (7분간 Progressive Download)
 * 
 * 성능 목표:
 * - 목표 동시 사용자: 86명 (계획서 기준)
 * - 세션 지속시간: 7분 (오디오 스트리밍)
 * - 최대 응답시간: 500ms (TestConfig.MAX_RESPONSE_TIME)
 * - 최대 에러율: 0.1% (TestConfig.MAX_ERROR_RATE)
 */
@RunWith(GrinderRunner)
class ScenarioBRecapStreaming {

    public static GTest authTest
    public static GTest recapListTest
    public static GTest recapDetailTest
    public static GTest recapAudioTest
    public static GTest audioStreamingTest
    
    public static HTTPRequest request
    public static AuthHelper authHelper
    public static MediaHelper mediaHelper
    public static MetricsCollector metricsCollector
    
    // 테스트 데이터
    private int userId
    private String selectedRecapId
    private String audioStreamingUrl
    
    @BeforeProcess
    public static void beforeProcess() {
        HTTPRequest.setConnectionTimeout(TestConfig.CONNECTION_TIMEOUT_MS)
        HTTPRequest.setReadTimeout(TestConfig.READ_TIMEOUT_MS)
        request = new HTTPRequest()
        
        // GTest 초기화
        authTest = new GTest(1, "Test Authentication")
        recapListTest = new GTest(2, "My Recap List")
        recapDetailTest = new GTest(3, "Recap Detail")
        recapAudioTest = new GTest(4, "Recap Audio URL")
        audioStreamingTest = new GTest(5, "Audio Streaming")
        
        // 헬퍼 클래스 초기화
        metricsCollector = new MetricsCollector()
        authHelper = new AuthHelper(request, metricsCollector)
        mediaHelper = new MediaHelper(request, metricsCollector)
        
        grinder.logger.info("=== Scenario B: Recap Streaming Load Test Initialized ===")
        grinder.logger.info("Target Virtual Users: ${TestConfig.TARGET_VU}")
        grinder.logger.info("Total Test Users: ${TestConfig.TOTAL_TEST_USERS}")
        grinder.logger.info("Audio streaming duration: ${TestConfig.SCENARIO_B_SESSION_DURATION / 60}min")
        grinder.logger.info("Audio chunk size: ${TestConfig.AUDIO_CHUNK_SIZE} bytes")
        grinder.logger.info("Streaming interval: ${TestConfig.STREAMING_INTERVAL_MS}ms")
        grinder.logger.info("Max response time: ${TestConfig.MAX_RESPONSE_TIME}ms")
        grinder.logger.info("Max error rate: ${TestConfig.MAX_ERROR_RATE * 100}%")
    }
    
    @BeforeThread
    public void beforeThread() {
        authTest.record(this, "authenticate")
        recapListTest.record(this, "getMyRecapList")
        recapDetailTest.record(this, "getRecapDetail")
        recapAudioTest.record(this, "getRecapAudioUrl")
        audioStreamingTest.record(this, "streamAudio")
        
        grinder.statistics.delayReports = true
        
        // 사용자 ID 할당 (1-6000 범위)
        userId = (grinder.threadNumber % TestConfig.TOTAL_TEST_USERS) + 1
        
        // 사용자별 기존 리캡 데이터 선택 (Phase 0에서 생성한 데이터)
        selectedRecapId = TestDataProvider.getExistingRecapId(userId)
        
        grinder.logger.info("Thread ${grinder.threadNumber}: User ${userId} assigned recap ${selectedRecapId}")
    }
    
    /**
     * 1단계: TestAuthService를 통한 인증
     */
    @Test
    public void authenticate() {
        try {
            grinder.logger.info("Authenticating user ${userId} using TestAuthService...")
            
            String accessToken = authHelper.authenticate(userId)
            
            if (accessToken != null) {
                grinder.logger.info("User ${userId} authenticated successfully")
            } else {
                throw new RuntimeException("Authentication failed for user ${userId}")
            }
            
        } catch (Exception e) {
            String errorMsg = "Authentication error for user ${userId}: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector.logError("AUTH_TEST_CALLBACK", errorMsg)
            throw e
        }
    }
    
    /**
     * 2단계: 내 리캡 목록 조회
     */
    @Test
    public void getMyRecapList() {
        try {
            HTTPResponse response = authHelper.authenticatedGet(
                ApiEndpoints.RECAP_MY_LIST, 
                "RECAP_MY_LIST"
            )
            
            if (response.statusCode == 200) {
                String responseBody = response.getText()
                grinder.logger.info("My recap list retrieved successfully for user ${userId}")
                
                // 응답에서 리캡 개수 확인 (로깅용)
                int recapCount = countRecapsInResponse(responseBody)
                grinder.logger.info("User ${userId} has ${recapCount} recaps available")
                
            } else {
                throw new RuntimeException("Failed to get my recap list: HTTP ${response.statusCode}")
            }
            
        } catch (Exception e) {
            String errorMsg = "My recap list error for user ${userId}: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector.logError("RECAP_MY_LIST", errorMsg)
            throw e
        }
    }
    
    /**
     * 3단계: 리캡 결과 상세 조회
     */
    @Test
    public void getRecapDetail() {
        try {
            String recapUrl = ApiEndpoints.getRecapResultUrl(selectedRecapId)
            
            HTTPResponse response = authHelper.authenticatedGet(
                recapUrl, 
                "RECAP_RESULT"
            )
            
            if (response.statusCode == 200) {
                String responseBody = response.getText()
                grinder.logger.info("Recap detail retrieved successfully for user ${userId}: ${selectedRecapId}")
                
                // 리캡 결과 검증 (기본적인 필드 존재 확인)
                validateRecapResponse(responseBody)
                
            } else {
                throw new RuntimeException("Failed to get recap detail: HTTP ${response.statusCode}")
            }
            
        } catch (Exception e) {
            String errorMsg = "Recap detail error for user ${userId}: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector.logError("RECAP_RESULT", errorMsg)
            throw e
        }
    }
    
    /**
     * 4단계: 리캡 오디오 URL 조회
     */
    @Test
    public void getRecapAudioUrl() {
        try {
            String audioUrl = ApiEndpoints.getRecapAudioUrl(selectedRecapId)
            
            HTTPResponse response = authHelper.authenticatedGet(
                audioUrl, 
                "RECAP_AUDIO"
            )
            
            if (response.statusCode == 200) {
                String responseBody = response.getText()
                
                // S3 직접 접근 URL 또는 스트리밍 URL 추출
                audioStreamingUrl = mediaHelper.extractAudioStreamingUrl(responseBody)
                
                if (audioStreamingUrl != null) {
                    grinder.logger.info("Audio streaming URL obtained for user ${userId}: ${audioStreamingUrl}")
                    
                    // URL 세션에 저장
                    grinder.properties["audioUrl_${userId}"] = audioStreamingUrl
                    
                } else {
                    throw new RuntimeException("Failed to extract audio streaming URL from response")
                }
                
            } else {
                throw new RuntimeException("Failed to get recap audio URL: HTTP ${response.statusCode}")
            }
            
        } catch (Exception e) {
            String errorMsg = "Recap audio URL error for user ${userId}: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector.logError("RECAP_AUDIO", errorMsg)
            throw e
        }
    }
    
    /**
     * 5단계: 오디오 스트리밍 (7분간 Progressive Download)
     */
    @Test
    public void streamAudio() {
        try {
            String streamingUrl = grinder.properties["audioUrl_${userId}"]
            
            if (streamingUrl == null) {
                throw new RuntimeException("Audio streaming URL not found - previous step may have failed")
            }
            
            grinder.logger.info("Starting audio streaming for user ${userId}: ${TestConfig.AUDIO_STREAMING_DURATION / 60}min session")
            
            // MediaHelper를 통한 오디오 스트리밍 실행
            boolean streamingSuccess = mediaHelper.streamAudio(
                streamingUrl, 
                TestConfig.AUDIO_STREAMING_DURATION
            )
            
            if (streamingSuccess) {
                grinder.logger.info("Audio streaming completed successfully for user ${userId}")
                
                // 세션 완료
                grinder.logger.info("=== Scenario B completed for user ${userId} ===")
                
            } else {
                throw new RuntimeException("Audio streaming failed or was interrupted")
            }
            
        } catch (Exception e) {
            String errorMsg = "Audio streaming error for user ${userId}: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector.logError("AUDIO_STREAMING", errorMsg)
            throw e
        }
    }
    
    /**
     * 응답에서 리캡 개수 카운트 (JSON 파싱)
     */
    private int countRecapsInResponse(String responseBody) {
        try {
            // 간단한 JSON 배열 요소 카운트
            if (responseBody.contains("[") && responseBody.contains("]")) {
                // "id" 필드 개수로 추정
                def idPattern = /"id"\s*:/
                def matcher = responseBody =~ idPattern
                return matcher.count
            }
            return 0
            
        } catch (Exception e) {
            grinder.logger.warn("Failed to count recaps in response: ${e.message}")
            return 0
        }
    }
    
    /**
     * 리캡 응답 기본 검증
     */
    private void validateRecapResponse(String responseBody) {
        try {
            // 기본적인 필드 존재 확인
            List<String> requiredFields = [
                "id", 
                "recap_result_id", 
                "member_id", 
                "storyboard_id"
            ]
            
            for (String field : requiredFields) {
                if (!responseBody.contains("\"${field}\"")) {
                    grinder.logger.warn("Missing required field in recap response: ${field}")
                }
            }
            
            // 리캡 상태 확인
            if (responseBody.contains("\"status\"")) {
                if (responseBody.contains("\"COMPLETED\"")) {
                    grinder.logger.info("Recap status: COMPLETED")
                } else if (responseBody.contains("\"PROCESSING\"")) {
                    grinder.logger.info("Recap status: PROCESSING")
                } else if (responseBody.contains("\"FAILED\"")) {
                    grinder.logger.warn("Recap status: FAILED")
                }
            }
            
        } catch (Exception e) {
            grinder.logger.warn("Recap response validation failed: ${e.message}")
        }
    }
    
    /**
     * 최종 성능 요약 출력
     */
    @Before
    public void printSessionSummary() {
        // 주기적으로 메트릭 요약 출력 (매 10번째 스레드에서만)
        if (grinder.threadNumber % 10 == 0) {
            metricsCollector.printRealTimeSummary()
        }
    }
    
    /**
     * 테스트 완료 후 최종 리포트
     */
    public void tearDown() {
        try {
            // 최종 성능 리포트 출력
            metricsCollector.printFinalReport()
            
            // 성능 목표 달성 여부 확인
            Map stats = metricsCollector.getStatistics()
            
            boolean errorRateAchieved = stats.overall.errorRate <= TestConfig.MAX_ERROR_RATE
            grinder.logger.info("Overall Performance Summary:")
            grinder.logger.info("- Error Rate Target: ${errorRateAchieved ? 'ACHIEVED' : 'FAILED'}")
            
            // API별 응답시간 목표 달성 여부
            stats.byApi.each { apiName, apiStats ->
                boolean responseTimeAchieved = apiStats.averageResponseTime <= TestConfig.MAX_RESPONSE_TIME
                grinder.logger.info("- ${apiName} Response Time: ${responseTimeAchieved ? 'ACHIEVED' : 'FAILED'}")
            }
            
        } catch (Exception e) {
            grinder.logger.error("TearDown error: ${e.message}")
        }
    }
}
