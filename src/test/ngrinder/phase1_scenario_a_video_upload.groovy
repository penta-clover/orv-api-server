package tests

import static net.grinder.script.Grinder.grinder
import static org.junit.Assert.*
import static org.hamcrest.Matchers.*
import net.grinder.plugin.http.HTTPRequest
import net.grinder.plugin.http.HTTPResponse
import net.grinder.script.GTest
import net.grinder.script.Grinder
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

import lib.TestConfig
import lib.ApiEndpoints
import lib.AuthHelper
import lib.MediaHelper
import lib.MetricsCollector

/**
 * Phase 1 Day 1 시나리오 A: 비디오 업로드 및 처리 부하 테스트
 * 
 * 테스트 시나리오:
 * 1. 사용자 로그인
 * 2. 스토리보드 목록 조회
 * 3. 스토리보드 선택
 * 4. 비디오 파일 업로드 (7분 480p, ~5MB)
 * 5. 업로드 상태 확인
 * 6. 처리 완료까지 대기
 * 
 * 부하 목표:
 * - 동시 사용자: 50명
 * - 테스트 지속시간: 10분
 * - 목표 TPS: 5 (비디오 업로드)
 * - 응답시간: P95 < 30초 (업로드), P95 < 2초 (조회)
 */
@RunWith(GrinderRunner)
class Phase1ScenarioAVideoUpload {

    public static GTest loginTest
    public static GTest storyboardListTest
    public static GTest videoUploadTest
    public static GTest uploadStatusTest
    
    public static HTTPRequest request
    public static AuthHelper authHelper
    public static MediaHelper mediaHelper
    public static MetricsCollector metricsCollector
    
    // 테스트 데이터
    private static File testVideoFile
    private static String[] testStoryboardIds
    private String currentAuthToken
    private String currentUserId
    
    @BeforeProcess
    public static void beforeProcess() {
        HTTPRequest.setConnectionTimeout(TestConfig.CONNECTION_TIMEOUT_MS)
        HTTPRequest.setReadTimeout(TestConfig.READ_TIMEOUT_MS)
        request = new HTTPRequest()
        
        // 테스트 초기화
        loginTest = new GTest(1, "User Login")
        storyboardListTest = new GTest(2, "Get Storyboard List")
        videoUploadTest = new GTest(3, "Video Upload")
        uploadStatusTest = new GTest(4, "Check Upload Status")
        
        // 헬퍼 클래스 초기화
        metricsCollector = new MetricsCollector()
        authHelper = new AuthHelper(request, metricsCollector)
        mediaHelper = new MediaHelper(request, metricsCollector)
        
        // 테스트 비디오 파일 준비
        prepareTestVideo()
        
        // 테스트용 스토리보드 ID 준비
        testStoryboardIds = TestConfig.TEST_STORYBOARD_IDS
        
        grinder.logger.info("Phase 1 Scenario A initialized - Video Upload Load Test")
        grinder.logger.info("Test video file: ${testVideoFile?.absolutePath}")
        grinder.logger.info("Available storyboards: ${testStoryboardIds?.length}")
    }
    
    @BeforeThread
    public void beforeThread() {
        loginTest.record(this, "login")
        storyboardListTest.record(this, "getStoryboardList")
        videoUploadTest.record(this, "uploadVideo")
        uploadStatusTest.record(this, "checkUploadStatus")
        
        grinder.statistics.delayReports = true
        grinder.logger.info("Thread ${grinder.threadNumber} started for Video Upload scenario")
    }
    
    @Before
    public void setUp() {
        // 각 테스트 전에 인증 상태 확인
        if (currentAuthToken == null || authHelper.isTokenExpired(currentAuthToken)) {
            performLogin()
        }
    }
    
    /**
     * 1단계: 사용자 로그인
     */
    @Test
    public void login() {
        performLogin()
    }
    
    /**
     * 2단계: 스토리보드 목록 조회
     */
    @Test
    public void getStoryboardList() {
        if (currentAuthToken == null) {
            performLogin()
        }
        
        try {
            Map<String, String> authHeaders = ["Authorization": "Bearer ${currentAuthToken}"]
            
            long startTime = System.currentTimeMillis()
            HTTPResponse response = request.GET(ApiEndpoints.STORYBOARD_LIST, authHeaders)
            long responseTime = System.currentTimeMillis() - startTime
            
            metricsCollector.recordResponse("STORYBOARD_LIST", responseTime, response.statusCode)
            
            if (response.statusCode == 200) {
                String responseBody = response.getText()
                grinder.logger.info("Storyboard list retrieved successfully (${responseTime}ms)")
                
                // 응답 시간 검증 (P95 < 2초)
                if (responseTime > 2000) {
                    grinder.logger.warn("Storyboard list response time exceeded target: ${responseTime}ms")
                }
                
            } else {
                String errorMsg = "Failed to get storyboard list: HTTP ${response.statusCode}"
                grinder.logger.error(errorMsg)
                metricsCollector.logError("STORYBOARD_LIST", errorMsg)
                fail(errorMsg)
            }
            
        } catch (Exception e) {
            String errorMsg = "Storyboard list error: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector.logError("STORYBOARD_LIST", errorMsg)
            fail(errorMsg)
        }
    }
    
    /**
     * 3-4단계: 비디오 업로드 (스토리보드 선택 포함)
     */
    @Test
    public void uploadVideo() {
        if (currentAuthToken == null) {
            performLogin()
        }
        
        if (testVideoFile == null || !testVideoFile.exists()) {
            fail("Test video file not available")
            return
        }
        
        try {
            // 랜덤 스토리보드 선택
            String selectedStoryboardId = selectRandomStoryboard()
            grinder.logger.info("Selected storyboard: ${selectedStoryboardId}")
            
            // 인증 헤더 준비
            Map<String, String> authHeaders = ["Authorization": "Bearer ${currentAuthToken}"]
            
            // 비디오 업로드 실행
            long uploadStartTime = System.currentTimeMillis()
            String videoId = mediaHelper.uploadVideo(testVideoFile, selectedStoryboardId, authHeaders)
            long uploadTime = System.currentTimeMillis() - uploadStartTime
            
            if (videoId != null) {
                grinder.logger.info("Video upload successful: videoId=${videoId}, uploadTime=${uploadTime}ms")
                
                // 응답 시간 검증 (P95 < 30초)
                if (uploadTime > 30000) {
                    grinder.logger.warn("Video upload time exceeded target: ${uploadTime}ms")
                }
                
                // 업로드 상태 확인
                checkVideoProcessingStatus(videoId, authHeaders)
                
            } else {
                String errorMsg = "Video upload failed - no videoId returned"
                grinder.logger.error(errorMsg)
                metricsCollector.logError("VIDEO_UPLOAD", errorMsg)
                fail(errorMsg)
            }
            
        } catch (Exception e) {
            String errorMsg = "Video upload error: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector.logError("VIDEO_UPLOAD", errorMsg)
            fail(errorMsg)
        }
    }
    
    /**
     * 5-6단계: 업로드 상태 확인 및 처리 완료 대기
     */
    @Test
    public void checkUploadStatus() {
        // 이 메서드는 uploadVideo() 내에서 호출되므로 별도 구현하지 않음
        grinder.logger.info("Upload status check completed within video upload process")
    }
    
    /**
     * 로그인 수행
     */
    private void performLogin() {
        try {
            // 테스트 사용자 정보 선택 (라운드 로빈)
            int userIndex = grinder.threadNumber % TestConfig.TEST_USERS.length
            def testUser = TestConfig.TEST_USERS[userIndex]
            
            grinder.logger.info("Logging in as user: ${testUser.email}")
            
            String authToken = authHelper.login(testUser.email, testUser.password)
            
            if (authToken != null) {
                currentAuthToken = authToken
                currentUserId = testUser.id
                grinder.logger.info("Login successful for user: ${testUser.email}")
            } else {
                String errorMsg = "Login failed for user: ${testUser.email}"
                grinder.logger.error(errorMsg)
                fail(errorMsg)
            }
            
        } catch (Exception e) {
            String errorMsg = "Login error: ${e.message}"
            grinder.logger.error(errorMsg)
            fail(errorMsg)
        }
    }
    
    /**
     * 랜덤 스토리보드 선택
     */
    private String selectRandomStoryboard() {
        if (testStoryboardIds == null || testStoryboardIds.length == 0) {
            throw new RuntimeException("No test storyboards available")
        }
        
        int randomIndex = new Random().nextInt(testStoryboardIds.length)
        return testStoryboardIds[randomIndex]
    }
    
    /**
     * 비디오 처리 상태 확인
     */
    private void checkVideoProcessingStatus(String videoId, Map<String, String> authHeaders) {
        try {
            int maxRetries = TestConfig.VIDEO_PROCESSING_MAX_RETRIES
            int retryInterval = TestConfig.VIDEO_PROCESSING_RETRY_INTERVAL_MS
            
            for (int i = 0; i < maxRetries; i++) {
                long startTime = System.currentTimeMillis()
                String statusUrl = "${ApiEndpoints.VIDEO_STATUS}/${videoId}"
                HTTPResponse response = request.GET(statusUrl, authHeaders)
                long responseTime = System.currentTimeMillis() - startTime
                
                metricsCollector.recordResponse("VIDEO_STATUS_CHECK", responseTime, response.statusCode)
                
                if (response.statusCode == 200) {
                    String responseBody = response.getText()
                    
                    // 처리 상태 확인 (JSON 파싱)
                    if (responseBody.contains('"status":"COMPLETED"') || 
                        responseBody.contains('"processing_status":"COMPLETED"')) {
                        grinder.logger.info("Video processing completed: ${videoId}")
                        return
                    } else if (responseBody.contains('"status":"FAILED"') || 
                              responseBody.contains('"processing_status":"FAILED"')) {
                        String errorMsg = "Video processing failed: ${videoId}"
                        grinder.logger.error(errorMsg)
                        metricsCollector.logError("VIDEO_PROCESSING", errorMsg)
                        return
                    } else {
                        grinder.logger.info("Video processing in progress: ${videoId} (retry ${i + 1}/${maxRetries})")
                    }
                } else {
                    grinder.logger.warn("Failed to check video status: HTTP ${response.statusCode}")
                }
                
                // 다음 체크까지 대기
                if (i < maxRetries - 1) {
                    Thread.sleep(retryInterval)
                }
            }
            
            grinder.logger.warn("Video processing status check timeout: ${videoId}")
            
        } catch (Exception e) {
            String errorMsg = "Video status check error: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector.logError("VIDEO_STATUS_CHECK", errorMsg)
        }
    }
    
    /**
     * 테스트 비디오 파일 준비
     */
    private static void prepareTestVideo() {
        try {
            // 테스트 비디오 파일 경로 확인
            String testVideoPath = TestConfig.TEST_VIDEO_PATH
            testVideoFile = new File(testVideoPath)
            
            if (!testVideoFile.exists()) {
                // 대체 경로들 시도
                String[] alternativePaths = [
                    "src/test/resources/videos/load-test-video.mp4",
                    "src/test/resources/videos/upload-test-video.mp4",
                    "test-data/video/sample-video.mp4"
                ]
                
                for (String altPath : alternativePaths) {
                    File altFile = new File(altPath)
                    if (altFile.exists()) {
                        testVideoFile = altFile
                        grinder.logger.info("Using alternative test video: ${altPath}")
                        break
                    }
                }
                
                if (testVideoFile == null || !testVideoFile.exists()) {
                    grinder.logger.error("No test video file found. Please prepare test video file.")
                    throw new RuntimeException("Test video file not found")
                }
            }
            
            grinder.logger.info("Test video prepared: ${testVideoFile.absolutePath} (${testVideoFile.length()} bytes)")
            
        } catch (Exception e) {
            grinder.logger.error("Failed to prepare test video: ${e.message}")
            throw e
        }
    }
}
