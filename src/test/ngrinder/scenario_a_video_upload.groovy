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
 * ORV API Server 부하테스트 시나리오 A: 비디오 업로드 및 리캡 생성
 * 
 * 테스트 플로우:
 * 1. TestAuthService를 통한 인증 (/auth/callback/test?code=test_user_1)
 * 2. 토픽 목록 조회와 내 아카이브 조회 (병렬)
 * 3. 사용자별 고정 스토리보드 선택 (해시 기반 분배)
 * 4. 스토리보드 상세 조회
 * 5. Scene별 순차 조회 (50초 딜레이)
 * 6. 비디오 업로드 (7분 480p, ~5MB)
 * 7. 리캡 예약 생성
 * 
 * 성능 목표:
 * - 목표 동시 사용자: 86명 (계획서 기준)
 * - 세션 지속시간: 8-11분 (스토리보드별 차별화)
 * - 최대 응답시간: 500ms (TestConfig.MAX_RESPONSE_TIME)
 * - 최대 에러율: 0.1% (TestConfig.MAX_ERROR_RATE)
 */
@RunWith(GrinderRunner)
class ScenarioAVideoUpload {

    public static GTest authTest
    public static GTest topicListTest
    public static GTest archiveTest
    public static GTest storyboardDetailTest
    public static GTest sceneDetailTest
    public static GTest videoUploadTest
    public static GTest recapReserveTest
    
    public static HTTPRequest request
    public static AuthHelper authHelper
    public static MediaHelper mediaHelper
    public static MetricsCollector metricsCollector
    
    // 테스트 데이터
    private static File testVideoFile
    private int userId
    private String storyboardId
    private int sessionDuration
    
    @BeforeProcess
    public static void beforeProcess() {
        HTTPRequest.setConnectionTimeout(TestConfig.CONNECTION_TIMEOUT_MS)
        HTTPRequest.setReadTimeout(TestConfig.READ_TIMEOUT_MS)
        request = new HTTPRequest()
        
        // GTest 초기화
        authTest = new GTest(1, "Test Authentication")
        topicListTest = new GTest(2, "Topic List")
        archiveTest = new GTest(3, "My Archive")
        storyboardDetailTest = new GTest(4, "Storyboard Detail")
        sceneDetailTest = new GTest(5, "Scene Detail")
        videoUploadTest = new GTest(6, "Video Upload")
        recapReserveTest = new GTest(7, "Recap Reserve")
        
        // 헬퍼 클래스 초기화
        metricsCollector = new MetricsCollector()
        authHelper = new AuthHelper(request, metricsCollector)
        mediaHelper = new MediaHelper(request, metricsCollector)
        
        // 테스트 비디오 파일 준비
        prepareTestVideo()
        
        // 테스트 데이터 분배 통계 출력
        TestDataProvider.printDistributionStats()
        
        grinder.logger.info("=== Scenario A: Video Upload Load Test Initialized ===")
        grinder.logger.info("Target Virtual Users: ${TestConfig.TARGET_VU}")
        grinder.logger.info("Total Test Users: ${TestConfig.TOTAL_TEST_USERS}")
        grinder.logger.info("Test video file: ${testVideoFile?.absolutePath}")
        grinder.logger.info("Max response time: ${TestConfig.MAX_RESPONSE_TIME}ms")
        grinder.logger.info("Max error rate: ${TestConfig.MAX_ERROR_RATE * 100}%")
    }
    
    @BeforeThread
    public void beforeThread() {
        authTest.record(this, "authenticate")
        topicListTest.record(this, "getTopicList")
        archiveTest.record(this, "getMyArchive")
        storyboardDetailTest.record(this, "getStoryboardDetail")
        sceneDetailTest.record(this, "getSceneDetail")
        videoUploadTest.record(this, "uploadVideo")
        recapReserveTest.record(this, "createRecapReservation")
        
        grinder.statistics.delayReports = true
        
        // 사용자 ID 할당 (1-6000 범위)
        userId = (grinder.threadNumber % TestConfig.TOTAL_TEST_USERS) + 1
        
        // 사용자별 고정 스토리보드 선택 (해시 기반 분배)
        storyboardId = TestDataProvider.getStoryboardByUserId(userId)
        sessionDuration = TestDataProvider.getSessionDuration(storyboardId)
        
        def storyboardInfo = TestDataProvider.getStoryboardInfo(storyboardId)
        grinder.logger.info("Thread ${grinder.threadNumber}: User ${userId} assigned to '${storyboardInfo.title}' (${storyboardInfo.sceneCount} scenes, ${sessionDuration/60}min session)")
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
     * 2단계: 토픽 목록 조회 (병렬 호출 1)
     */
    @Test
    public void getTopicList() {
        try {
            HTTPResponse response = authHelper.authenticatedGet(
                ApiEndpoints.TOPIC_LIST, 
                "TOPIC_LIST"
            )
            
            if (response.statusCode == 200) {
                grinder.logger.info("Topic list retrieved successfully for user ${userId}")
            } else {
                throw new RuntimeException("Failed to get topic list: HTTP ${response.statusCode}")
            }
            
        } catch (Exception e) {
            String errorMsg = "Topic list error for user ${userId}: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector.logError("TOPIC_LIST", errorMsg)
            throw e
        }
    }
    
    /**
     * 2단계: 내 아카이브 조회 (병렬 호출 2)
     */
    @Test
    public void getMyArchive() {
        try {
            HTTPResponse response = authHelper.authenticatedGet(
                ApiEndpoints.ARCHIVE_MY_VIDEOS, 
                "ARCHIVE_MY_VIDEOS"
            )
            
            if (response.statusCode == 200) {
                grinder.logger.info("My archive retrieved successfully for user ${userId}")
            } else {
                throw new RuntimeException("Failed to get my archive: HTTP ${response.statusCode}")
            }
            
        } catch (Exception e) {
            String errorMsg = "My archive error for user ${userId}: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector.logError("ARCHIVE_MY_VIDEOS", errorMsg)
            throw e
        }
    }
    
    /**
     * 3단계: 스토리보드 상세 조회
     */
    @Test
    public void getStoryboardDetail() {
        try {
            String storyboardUrl = ApiEndpoints.getStoryboardDetailUrl(storyboardId)
            
            HTTPResponse response = authHelper.authenticatedGet(
                storyboardUrl, 
                "STORYBOARD_DETAIL"
            )
            
            if (response.statusCode == 200) {
                def storyboardInfo = TestDataProvider.getStoryboardInfo(storyboardId)
                grinder.logger.info("Storyboard '${storyboardInfo.title}' retrieved for user ${userId}")
            } else {
                throw new RuntimeException("Failed to get storyboard detail: HTTP ${response.statusCode}")
            }
            
        } catch (Exception e) {
            String errorMsg = "Storyboard detail error for user ${userId}: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector.logError("STORYBOARD_DETAIL", errorMsg)
            throw e
        }
    }
    
    /**
     * 4단계: Scene별 순차 조회 (50초 딜레이)
     */
    @Test
    public void getSceneDetail() {
        try {
            int sceneCount = TestDataProvider.getSceneCount(storyboardId)
            grinder.logger.info("Starting scene sequence for user ${userId}: ${sceneCount} scenes")
            
            for (int sceneIndex = 1; sceneIndex <= sceneCount; sceneIndex++) {
                // Scene ID 생성 (실제 Scene ID 패턴에 맞게 조정 필요)
                String sceneId = "scene_${storyboardId}_${String.format('%02d', sceneIndex)}"
                String sceneUrl = ApiEndpoints.getSceneDetailUrl(storyboardId, sceneId)
                
                HTTPResponse response = authHelper.authenticatedGet(
                    sceneUrl, 
                    "SCENE_DETAIL_${sceneIndex}"
                )
                
                if (response.statusCode == 200) {
                    grinder.logger.info("Scene ${sceneIndex}/${sceneCount} completed for user ${userId}")
                } else {
                    grinder.logger.warn("Scene ${sceneIndex} failed for user ${userId}: HTTP ${response.statusCode}")
                }
                
                // Scene간 50초 딜레이 (마지막 Scene 제외)
                if (sceneIndex < sceneCount) {
                    grinder.logger.info("Waiting ${TestConfig.SCENE_DELAY_SECONDS}s before next scene...")
                    Thread.sleep(TestConfig.SCENE_DELAY_SECONDS * 1000)
                }
            }
            
            grinder.logger.info("All scenes completed for user ${userId}")
            
        } catch (Exception e) {
            String errorMsg = "Scene detail error for user ${userId}: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector.logError("SCENE_DETAIL", errorMsg)
            throw e
        }
    }
    
    /**
     * 5단계: 비디오 업로드
     */
    @Test
    public void uploadVideo() {
        try {
            if (testVideoFile == null || !testVideoFile.exists()) {
                throw new RuntimeException("Test video file not available")
            }
            
            grinder.logger.info("Starting video upload for user ${userId} with storyboard ${storyboardId}")
            
            Map<String, String> authHeaders = authHelper.getAuthHeadersForMultipart()
            
            String videoId = mediaHelper.uploadVideo(testVideoFile, storyboardId, authHeaders)
            
            if (videoId != null) {
                grinder.logger.info("Video upload successful for user ${userId}: videoId=${videoId}")
                
                // 비디오 ID를 세션에 저장 (리캡 예약에서 사용)
                grinder.properties["videoId_${userId}"] = videoId
                
            } else {
                throw new RuntimeException("Video upload failed - no videoId returned")
            }
            
        } catch (Exception e) {
            String errorMsg = "Video upload error for user ${userId}: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector.logError("VIDEO_UPLOAD", errorMsg)
            throw e
        }
    }
    
    /**
     * 6단계: 리캡 예약 생성
     */
    @Test
    public void createRecapReservation() {
        try {
            String videoId = grinder.properties["videoId_${userId}"]
            
            if (videoId == null) {
                throw new RuntimeException("Video ID not found - upload may have failed")
            }
            
            // 리캡 예약 요청 바디 생성
            String requestBody = """
            {
                "videoId": "${videoId}",
                "storyboardId": "${storyboardId}"
            }
            """.trim()
            
            HTTPResponse response = authHelper.authenticatedPost(
                ApiEndpoints.RECAP_RESERVE, 
                requestBody, 
                "RECAP_RESERVE"
            )
            
            if (response.statusCode == 200 || response.statusCode == 201) {
                grinder.logger.info("Recap reservation created successfully for user ${userId}")
                
                // 세션 완료
                grinder.logger.info("=== Scenario A completed for user ${userId} ===")
                
            } else {
                throw new RuntimeException("Failed to create recap reservation: HTTP ${response.statusCode}")
            }
            
        } catch (Exception e) {
            String errorMsg = "Recap reservation error for user ${userId}: ${e.message}"
            grinder.logger.error(errorMsg)
            metricsCollector.logError("RECAP_RESERVE", errorMsg)
            throw e
        }
    }
    
    /**
     * 테스트 비디오 파일 준비
     */
    private static void prepareTestVideo() {
        try {
            // 기본 비디오 파일명
            String videoFileName = TestConfig.VIDEO_FILE_NAME  // "sample-480p-7min.mp4"
            
            // 가능한 경로들 확인
            String[] possiblePaths = [
                "src/test/ngrinder/resources/test-videos/${videoFileName}",
                "src/test/resources/videos/upload-test-video.mp4",
                "src/test/resources/videos/extract-test-video.mp4",
                videoFileName  // 현재 디렉토리
            ]
            
            for (String path : possiblePaths) {
                File file = new File(path)
                if (file.exists()) {
                    testVideoFile = file
                    grinder.logger.info("Test video found: ${file.absolutePath} (${file.length()} bytes)")
                    
                    // 파일 크기 검증 (너무 크면 경고)
                    long fileSizeMB = file.length() / (1024 * 1024)
                    if (fileSizeMB > TestConfig.VIDEO_FILE_SIZE_MB * 2) {
                        grinder.logger.warn("Test video file is larger than expected: ${fileSizeMB}MB > ${TestConfig.VIDEO_FILE_SIZE_MB * 2}MB")
                    }
                    
                    return
                }
            }
            
            // 비디오 파일을 찾을 수 없는 경우 경고
            grinder.logger.warn("Test video file not found. Trying to continue with mock data...")
            grinder.logger.warn("Expected locations: ${possiblePaths}")
            
            // 테스트용 더미 파일 생성
            testVideoFile = createDummyVideoFile()
            
        } catch (Exception e) {
            grinder.logger.error("Failed to prepare test video: ${e.message}")
            throw e
        }
    }
    
    /**
     * 테스트용 더미 비디오 파일 생성
     */
    private static File createDummyVideoFile() {
        try {
            File dummyFile = new File("dummy-test-video.mp4")
            
            if (!dummyFile.exists()) {
                // 약 5MB 크기의 더미 데이터 생성
                byte[] dummyData = new byte[TestConfig.VIDEO_FILE_SIZE_MB * 1024 * 1024]
                new Random().nextBytes(dummyData)
                
                dummyFile.withOutputStream { os ->
                    os.write(dummyData)
                }
                
                grinder.logger.info("Created dummy test video: ${dummyFile.absolutePath} (${dummyFile.length()} bytes)")
            }
            
            return dummyFile
            
        } catch (Exception e) {
            grinder.logger.error("Failed to create dummy video file: ${e.message}")
            throw e
        }
    }
}
