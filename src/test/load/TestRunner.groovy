import static net.grinder.script.Grinder.grinder

import java.security.SecureRandom
import java.time.ZonedDateTime
import java.time.ZoneId
import java.nio.file.Files
import java.nio.file.Paths

import net.grinder.script.GTest
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import org.ngrinder.http.HTTPRequest
import org.ngrinder.http.HTTPRequestControl
import org.ngrinder.http.HTTPResponse
import org.ngrinder.http.cookie.Cookie
import org.ngrinder.http.cookie.CookieManager
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(GrinderRunner)
class TestRunner {

    private static GTest getAuthUrlTest
    private static GTest getAuthCallbackTest
    private static GTest postJoinTest
    private static GTest getTopicListTest
    private static GTest getMyVideosTest
    private static GTest getStoryboardPreviewTest
    private static GTest getStoryboardTest
    private static GTest getSceneTest
    private static GTest uploadVideoTest
    private static GTest createRecapTest

    private static HTTPRequest request
    private static final Map<String, String> HEADERS = [:]
    private static final List<Cookie> COOKIES = []

    private static byte[] sharedVideoData // 업로드 테스트용 영상 데이터

    private String testUserId

    @BeforeProcess
    static void beforeProcess() {
        HTTPRequestControl.connectionTimeout = 30000
        HTTPRequestControl.socketTimeout = 30000

        getAuthUrlTest = new GTest(1, 'GetAuthUrl')
        getAuthCallbackTest = new GTest(2, 'GetAuthCallback')
        postJoinTest = new GTest(3, 'PostJoin')
        getTopicListTest = new GTest(4, 'GetTopicList')
        getMyVideosTest = new GTest(5, 'GetMyVideos')
        getStoryboardPreviewTest = new GTest(6, 'GetStoryboardPreview')
        getStoryboardTest = new GTest(7, 'GetStoryboard')
        getSceneTest = new GTest(8, 'GetScene')
        uploadVideoTest = new GTest(9, 'UploadVideo')
        createRecapTest = new GTest(10, 'CreateRecap')

        request = new HTTPRequest()

        def videoPath = Paths.get("./resources/upload-test-video.mp4")
        if (Files.exists(videoPath)) {
            sharedVideoData = Files.readAllBytes(videoPath)
            grinder.logger.info("Loaded shared video data: ${sharedVideoData.length / (1024 * 1024)} MB")
        }
    }

    @BeforeThread
    void beforeThread() {
        int agentNumber = grinder.agentNumber ?: 0
        int processNumber = grinder.processNumber ?: 0
        int threadNumber = grinder.threadNumber ?: 0

        this.testUserId = "test_user_${agentNumber}_${processNumber}_${threadNumber}"
        grinder.logger.info("Generated unique user ID: ${testUserId}")

        getAuthUrlTest.record(this, 'getAuthUrl')
        getAuthCallbackTest.record(this, 'getAuthCallback')
        postJoinTest.record(this, 'postJoin')
        getTopicListTest.record(this, 'getTopicList')
        getMyVideosTest.record(this, 'getMyVideos')
        getStoryboardPreviewTest.record(this, 'getStoryboardPreview')
        getStoryboardTest.record(this, 'getStoryboard')
        getSceneTest.record(this, 'getScene')
        uploadVideoTest.record(this, 'uploadVideo')
        createRecapTest.record(this, 'createRecapReservation')

        grinder.statistics.delayReports = true
    }

    @Before
    void before() {
        request.headers = HEADERS
        CookieManager.addCookies(COOKIES)
        grinder.logger.info('before. init headers and cookies')
    }

    @Test
    void scenarioA() {
        SecureRandom random = new SecureRandom()

        // 1. 사용자 인증
        getAuthUrl()
        HTTPResponse authResponse = getAuthCallback(testUserId)

        // JWT 토큰 추출 & 헤더에 저장
        String location = authResponse.getHeader('Location')
        if (location != null && location.contains('jwtToken=')) {
            int tokenStart = location.indexOf('jwtToken=') + 9
            int tokenEnd = location.indexOf('&', tokenStart)
            if (tokenEnd == -1) tokenEnd = location.length()

            String jwtToken = location.substring(tokenStart, tokenEnd)
            grinder.logger.info('JWT extracted: ' + jwtToken.substring(0, Math.min(20, jwtToken.length())) + '...')

            HEADERS['Authorization'] = 'Bearer ' + jwtToken
            request.headers = HEADERS
        } else {
            grinder.logger.error("Failed to extract JWT token from response location: ${location}")
            assert location?.contains('jwtToken=') : "JWT token should be present in Location header"
        }

        // 신규 유저인 경우 회원가입
        if (location && location.contains('isNewUser=true')) {
            grinder.logger.info('New user detected, proceeding with join...')

            int month = 1 + random.nextInt(12)
            int day = 1 + random.nextInt(28)
            String monthStr = month < 10 ? "0${month}" : "${month}"
            String dayStr = day < 10 ? "0${day}" : "${day}"

            def joinData = [
                nickname: "t${testUserId.substring(10, Math.min(16, testUserId.length()))}",
                gender: random.nextBoolean() ? 'M' : 'F',
                birthDay: "199${random.nextInt(10)}-${monthStr}-${dayStr}",
                phoneNumber: "010${1000 + random.nextInt(9000)}${1000 + random.nextInt(9000)}"
            ]

            postJoin(joinData)
            Thread.sleep(500)
        }

        // 2. 대시보드 로딩
        getTopicList()
        Thread.sleep(500)

        getMyVideos()
        Thread.sleep(500)

        // 스토리보드 미리보기
        getStoryboardPreview('0afecfc8-62a4-4398-85a8-0cff8b8f698f')
        Thread.sleep(1500)
        getStoryboardPreview('9c570f84-16b6-4c5d-85b0-eadf05829056')
        Thread.sleep(1500)
        getStoryboardPreview('18779df7-a80d-497c-9206-9e61540bb465')
        Thread.sleep(1500)
        getStoryboardPreview('8c2746c4-4613-47f8-8799-235fec7f359d')
        Thread.sleep(1500)

        HTTPResponse storyboardResponse = getStoryboard('8c2746c4-4613-47f8-8799-235fec7f359d')
        Thread.sleep(1000)

        // 3. Scene 상세 조회
        String usedStoryboardId = '8c2746c4-4613-47f8-8799-235fec7f359d'  // 사용한 스토리보드 ID 저장

        if (storyboardResponse.getBodyText()) {
            JsonSlurper jsonSlurper = new JsonSlurper()
            def responseData = jsonSlurper.parseText(storyboardResponse.getBodyText())

            if (responseData?.data?.startSceneId) {
                String currentSceneId = responseData.data.startSceneId

                HTTPResponse sceneResponse = getScene(currentSceneId)
                if (sceneResponse.getBodyText()) {
                    def sceneData = jsonSlurper.parseText(sceneResponse.getBodyText())

                    if (sceneData?.data?.sceneType != 'END') {
                        // 사용자가 질문을 읽고 답변하는 시간 시뮬레이션
                        Integer thinkingTime = 45000 + random.nextInt(10000)
                        grinder.logger.info("User thinking time: ${thinkingTime/1000} seconds")
                        Thread.sleep(thinkingTime)
                    }
                }
            }
        }

        // 4. 영상 업로드
        grinder.logger.info("=== Starting Video Upload Phase ===")
        HTTPResponse uploadResponse = uploadVideo(usedStoryboardId)
        Thread.sleep(1000)

        // 5. 리캡 예약 생성
        if (uploadResponse.getBodyText()) {
            JsonSlurper jsonSlurper = new JsonSlurper()
            def uploadData = jsonSlurper.parseText(uploadResponse.getBodyText())
            String videoId = uploadData?.data

            if (videoId) {
                grinder.logger.info("Video uploaded with ID: ${videoId}")

                // 리캡 예약 생성
                grinder.logger.info("Creating recap reservation...")
                HTTPResponse recapResponse = createRecapReservation(videoId)

                if (recapResponse.getBodyText()) {
                    def recapData = jsonSlurper.parseText(recapResponse.getBodyText())
                    grinder.logger.info("Recap reservation created: ${recapData}")
                }

                Thread.sleep(2000)
            } else {
                grinder.logger.error("Failed to extract video ID from upload response")
            }
        } else {
            grinder.logger.error("Empty response from video upload")
        }

        grinder.logger.info("=== Scenario A Completed ===")
    }

    HTTPResponse getAuthUrl() {
        HTTPResponse response = request.GET('https://api.orv.im/api/v0/auth/login/test')
        assert response.statusCode == 302 : "Auth URL should redirect"
        assert response.getHeader('Location') != null : "Should have Location header"
        return response
    }

    HTTPResponse getAuthCallback(String testUserId) {
        HTTPResponse response = request.GET("https://api.orv.im/api/v0/auth/callback/test?code=${testUserId}")
        assert response.statusCode == 302 : "Callback should redirect"
        assert response.getHeader('Location') != null : "Should have Location header"
        return response
    }

    HTTPResponse postJoin(Map joinData) {
        def jsonBuilder = new JsonBuilder(joinData)

        HTTPResponse response = request.POST(
            "https://api.orv.im/api/v0/auth/join",
            jsonBuilder.toString().getBytes('UTF-8')
        )

        assert response.statusCode == 200 : "Join should succeed"
        return response
    }

    HTTPResponse getTopicList() {
        HTTPResponse response = request.GET('https://api.orv.im/api/v0/topic/list')
        assert response.statusCode == 200 : "Topic list should load"
        return response
    }

    HTTPResponse getMyVideos() {
        HTTPResponse response = request.GET('https://api.orv.im/api/v0/archive/videos/my')
        assert response.statusCode == 200 : "My videos should load"
        return response
    }

    HTTPResponse getStoryboardPreview(String storyboardId) {
        HTTPResponse response = request.GET("https://api.orv.im/api/v0/storyboard/${storyboardId}/preview")
        assert response.statusCode == 200 : "Storyboard preview should load"
        return response
    }

    HTTPResponse getStoryboard(String storyboardId) {
        HTTPResponse response = request.GET("https://api.orv.im/api/v0/storyboard/${storyboardId}")
        assert response.statusCode == 200 : "Storyboard should load"
        return response
    }

    HTTPResponse getScene(String sceneId) {
        HTTPResponse response = request.GET("https://api.orv.im/api/v0/storyboard/scene/${sceneId}")
        assert response.statusCode == 200 : "Scene should load"
        return response
    }

    HTTPResponse uploadVideo(String storyboardId) {
        // nGrinder 리소스에서 비디오 파일 로드
        def videoData = sharedVideoData

        if (videoData == null) {
            grinder.logger.error("Shared video data not loaded")
            assert false : "Video data not available"
        }

        grinder.logger.info("Video file size: ${videoData.length / (1024 * 1024)} MB")

        // Multipart 요청 생성
        def boundary = "----WebKitFormBoundary${System.currentTimeMillis()}"
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, 'UTF-8'), true)

        // storyboardId 파라미터
        writer.append("--${boundary}\r\n")
        writer.append('Content-Disposition: form-data; name="storyboardId"\r\n')
        writer.append('\r\n')
        writer.append("${storyboardId}\r\n")

        // video 파일
        writer.append("--${boundary}\r\n")
        writer.append('Content-Disposition: form-data; name="video"; filename="recording.mp4"\r\n')
        writer.append('Content-Type: video/mp4\r\n')
        writer.append('\r\n')
        writer.flush()

        baos.write(videoData)

        writer.append('\r\n')
        writer.append("--${boundary}--\r\n")
        writer.flush()

        // 헤더 설정
        Map<String, String> uploadHeaders = new HashMap<>(HEADERS)
        uploadHeaders['Content-Type'] = "multipart/form-data; boundary=${boundary}".toString()

        HTTPRequest uploadRequest = new HTTPRequest()
        uploadRequest.headers = uploadHeaders

        grinder.logger.info("Starting video upload...")
        HTTPResponse response = uploadRequest.POST(
            'https://api.orv.im/api/v0/archive/recorded-video',
            baos.toByteArray()
        )

        assert response.statusCode == 200 : "Video upload should succeed (got ${response.statusCode})"
        grinder.logger.info("Video uploaded successfully")
        return response
    }

    HTTPResponse createRecapReservation(String videoId) {
        JsonBuilder jsonBuilder = new JsonBuilder([
            videoId: videoId,
            scheduledAt: ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                .plusWeeks(1)
                .withHour(19)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toString()
        ])

        Map<String, String> recapHeaders = new HashMap<>(HEADERS)
        recapHeaders['Content-Type'] = 'application/json'

        HTTPRequest recapRequest = new HTTPRequest()
        recapRequest.headers = recapHeaders

        grinder.logger.info("Creating recap reservation for video ${videoId}")
        HTTPResponse response = recapRequest.POST(
            'https://api.orv.im/api/v0/reservation/recap/video',
            jsonBuilder.toString().getBytes('UTF-8')
        )

        assert response.statusCode == 200 : "Recap reservation should succeed (got ${response.statusCode})"
        grinder.logger.info("Recap reservation created successfully")
        return response
    }
}
