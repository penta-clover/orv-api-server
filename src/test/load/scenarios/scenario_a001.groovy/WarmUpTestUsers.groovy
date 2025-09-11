import static net.grinder.script.Grinder.grinder
import static org.junit.Assert.*
import static org.hamcrest.Matchers.*
import net.grinder.script.GTest
import net.grinder.script.Grinder
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread

import groovy.json.JsonSlurper

// import static net.grinder.util.GrinderUtils.* // You can use this if you're using nGrinder after 3.2.3
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

import org.ngrinder.http.HTTPRequest
import org.ngrinder.http.HTTPRequestControl
import org.ngrinder.http.HTTPResponse
import org.ngrinder.http.cookie.Cookie
import org.ngrinder.http.cookie.CookieManager

/**
 * 부하테스트를 위한 테스트 사용자 사전 생성 스크립트
 *
 * 실행 방법:
 * - vUser: 50 (각 유저가 120명씩 생성)
 * - Duration: Run Count 1
 * - 예상 소요시간: 10-15분
 *
 * 생성되는 사용자:
 * - test_user_0_0_0 ~ test_user_5_9_99 (총 6,000명)
 * - agent: 0-5, process: 0-9, thread: 0-99
 *
 * @author admin
 */
@CompileStatic
@RunWith(GrinderRunner)
class WarmUpTestUsers {

    private static GTest authUrlTest
    private static GTest authCallbackTest

    private static HTTPRequest request
    private static final Map<String, String> HEADERS = [:]
    private static final List<Cookie> COOKIES = []

    // 각 vUser가 생성할 사용자 수
    private static final int USERS_PER_VUSER = 120
    // DB 부하 분산을 위한 대기시간 (ms)
    private static final int DELAY_BETWEEN_USERS = 100

    private int startIndex
    private int endIndex

    @BeforeProcess
    static void beforeProcess() {
        HTTPRequestControl.connectionTimeout = 30000
        HTTPRequestControl.socketTimeout = 30000

        authUrlTest = new GTest(1, 'GetAuthUrl')
        authCallbackTest = new GTest(2, 'GetAuthCallback')

        request = new HTTPRequest()

        grinder.logger.info('=== Warm-up Process Started ===')
        grinder.logger.info("Agent: ${grinder.agentNumber}, Process: ${grinder.processNumber}")
    }

    @BeforeThread
    void beforeThread() {
        authUrlTest.record(this, 'getAuthUrl')
        authCallbackTest.record(this, 'getAuthCallback')

        grinder.statistics.delayReports = true

        // 각 스레드가 담당할 사용자 인덱스 계산
        int totalThreadNumber = grinder.agentNumber * 1000 + grinder.threadNumber
        startIndex = totalThreadNumber * USERS_PER_VUSER
        endIndex = startIndex + USERS_PER_VUSER

        grinder.logger.info("Thread ${grinder.threadNumber} will create users from ${startIndex} to ${endIndex - 1}")
    }

    @Before
    void before() {
        request.headers = HEADERS
        CookieManager.addCookies(COOKIES)
    }

    @Test
    void getTestUsers() {
        int successCount = 0
        int failCount = 0

        grinder.logger.info("Starting user creation for thread ${grinder.threadNumber}")

        // 할당된 범위의 사용자 생성
        for (int i = startIndex; i < endIndex; i++) {
            // 인덱스를 agent/process/thread로 변환
            int agent = i / 1000
            int process = (i % 1000) / 100
            int thread = i % 100

            // 범위 체크 (agent: 0-5, process: 0-9, thread: 0-99)
            if (agent > 5 || process > 9 || thread > 99) {
                grinder.logger.info("Skipping out of range user: ${agent}_${process}_${thread}")
                continue
            }

            String testUserId = "test_user_${agent}_${process}_${thread}"

            try {
                // 테스트 사용자로 로그인 (자동 회원가입)
                boolean success = createUser(testUserId)

                if (success) {
                    successCount++
                    if (successCount % 10 == 0) {
                        grinder.logger.info("Progress: ${successCount}/${USERS_PER_VUSER} users created")
                    }
                } else {
                    failCount++
                    grinder.logger.warn("Failed to create user: ${testUserId}")
                }

                // DB 부하 분산을 위한 대기
                Thread.sleep(DELAY_BETWEEN_USERS)

            } catch (Exception e) {
                failCount++
                grinder.logger.error("Error creating user ${testUserId}: ${e.message}")
            }
        }

        grinder.logger.info("=== Thread ${grinder.threadNumber} Completed ===")
        grinder.logger.info("Success: ${successCount}, Failed: ${failCount}")

        // 최소 90% 성공률 확인
        double successRate = successCount.toDouble() / USERS_PER_VUSER.toDouble()
        assertThat('Success rate should be at least 90%', successRate, greaterThanOrEqualTo(0.9))
    }

    private boolean createUser(String testUserId) {
        try {
            // 1. Auth URL 획득 (실제로는 사용 안함)
            HTTPResponse authUrlResponse = getAuthUrl()
            if (authUrlResponse.statusCode != 302) {
                grinder.logger.error("Auth URL failed for ${testUserId}: ${authUrlResponse.statusCode}")
                return false
            }

            // 2. Callback으로 로그인 (자동 회원가입)
            HTTPResponse callbackResponse = getAuthCallback(testUserId)
            if (callbackResponse.statusCode != 302) {
                grinder.logger.error("Auth callback failed for ${testUserId}: ${callbackResponse.statusCode}")
                return false
            }

            // JWT 토큰 확인
            String location = callbackResponse.getHeader('Location')
            if (location != null && location.contains('jwtToken=')) {
                grinder.logger.debug("User ${testUserId} created successfully")
                return true
            } else {
                grinder.logger.error("No JWT token in response for ${testUserId}")
                return false
            }

        } catch (Exception e) {
            grinder.logger.error("Exception creating user ${testUserId}: ${e.message}")
            return false
        }
    }

    HTTPResponse getAuthUrl() {
        HTTPResponse response = request.GET('https://api.orv.im/api/v0/auth/login/test')
        return response
    }

    HTTPResponse getAuthCallback(String testUserId) {
        HTTPResponse response = request.GET("https://api.orv.im/api/v0/auth/callback/test?code=${testUserId}")
        return response
    }
}
