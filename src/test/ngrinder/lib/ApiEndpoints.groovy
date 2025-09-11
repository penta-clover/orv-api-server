package lib

/**
 * ORV API Server 엔드포인트 상수 정의
 * Phase 1에서 구현된 API 엔드포인트 관리 클래스
 */
@CompileStatic
class ApiEndpoints {

    static final String BASE = TestConfig.BASE_URL + TestConfig.API_BASE_PATH

    // ========== 인증 관련 ==========
    /**
     * 테스트 인증 콜백 (Phase 0에서 구현한 TestAuthService 사용)
     * 사용법: GET /api/v0/auth/callback/test?code=test_user_1
     */
    static final String AUTH_TEST_CALLBACK = "${BASE}/auth/callback/test"

    // ========== 대시보드 관련 ==========
    /**
     * 토픽 목록 조회 (시나리오 A에서 병렬 호출)
     * 사용법: GET /api/v0/topic/list
     */
    static final String TOPIC_LIST = "${BASE}/topic/list"

    /**
     * 내 비디오 아카이브 조회 (시나리오 A에서 병렬 호출)
     * 사용법: GET /api/v0/archive/videos/my
     */
    static final String ARCHIVE_MY_VIDEOS = "${BASE}/archive/videos/my"

    // ========== 스토리보드 관련 ==========
    /**
     * 스토리보드 상세 조회
     * 사용법: GET /api/v0/storyboard/{storyboardId}
     */
    static final String STORYBOARD_DETAIL = "${BASE}/storyboard"  // /{storyboardId}

    /**
     * 개별 Scene 조회 (기존 계획)
     * 사용법: GET /api/v0/storyboard/{storyboardId}/scene/{sceneId}
     */
    static final String SCENE_DETAIL = "${BASE}/storyboard"      // /{storyboardId}/scene/{sceneId}

    /**
     * 전체 Scene 조회 (최적화 옵션 - Phase 2에서 선택적 사용)
     * 사용법: GET /api/v0/storyboard/{storyboardId}/scene/all
     * 효과: 네트워크 요청 수 85% 감소 가능
     */
    static final String SCENE_ALL = "${BASE}/storyboard"         // /{storyboardId}/scene/all

    // ========== 비디오 관련 ==========
    /**
     * 비디오 업로드 (시나리오 A 핵심)
     * 사용법: POST /api/v0/video/upload (multipart/form-data)
     * 파일: 7분 480p 영상 (~5MB)
     */
    static final String VIDEO_UPLOAD = "${BASE}/video/upload"

    // ========== 리캡 관련 ==========
    /**
     * 리캡 예약 생성 (시나리오 A 마지막 단계)
     * 사용법: POST /api/v0/reservation/recap/reserve
     * Body: { "videoId": "uuid", "storyboardId": "uuid" }
     */
    static final String RECAP_RESERVE = "${BASE}/reservation/recap/reserve"

    /**
     * 내 리캡 목록 조회 (시나리오 B 시작)
     * 사용법: GET /api/v0/archive/recap/my
     */
    static final String RECAP_MY_LIST = "${BASE}/archive/recap/my"

    /**
     * 리캡 결과 조회 (시나리오 B 핵심)
     * 사용법: GET /api/v0/archive/recap/{recapId}
     */
    static final String RECAP_RESULT = "${BASE}/archive/recap"   // /{recapId}

    /**
     * 리캡 오디오 스트리밍 (시나리오 B 핵심)
     * 사용법: GET /api/v0/archive/recap/{recapId}/audio
     * 응답: S3 직접 접근 URL 또는 스트리밍 데이터
     */
    static final String RECAP_AUDIO = "${BASE}/archive/recap"    // /{recapId}/audio

    // ========== 헬퍼 메소드 ==========
    /**
     * 스토리보드 상세 URL 생성
     * @param storyboardId 스토리보드 UUID
     * @return 완전한 URL
     */
    static String getStoryboardDetailUrl(String storyboardId) {
        return "${STORYBOARD_DETAIL}/${storyboardId}"
    }

    /**
     * Scene 상세 URL 생성
     * @param storyboardId 스토리보드 UUID
     * @param sceneId Scene UUID
     * @return 완전한 URL
     */
    static String getSceneDetailUrl(String storyboardId, String sceneId) {
        return "${SCENE_DETAIL}/${storyboardId}/scene/${sceneId}"
    }

    /**
     * 전체 Scene 조회 URL 생성
     * @param storyboardId 스토리보드 UUID
     * @return 완전한 URL
     */
    static String getSceneAllUrl(String storyboardId) {
        return "${SCENE_ALL}/${storyboardId}/scene/all"
    }

    /**
     * 리캡 결과 조회 URL 생성
     * @param recapId 리캡 UUID
     * @return 완전한 URL
     */
    static String getRecapResultUrl(String recapId) {
        return "${RECAP_RESULT}/${recapId}"
    }

    /**
     * 리캡 오디오 URL 생성
     * @param recapId 리캡 UUID
     * @return 완전한 URL
     */
    static String getRecapAudioUrl(String recapId) {
        return "${RECAP_AUDIO}/${recapId}/audio"
    }

    /**
     * 테스트 인증 URL 생성
     * @param userId 테스트 사용자 ID (1-6000)
     * @return 완전한 인증 URL
     */
    static String getTestAuthUrl(int userId) {
        return "${AUTH_TEST_CALLBACK}?code=${TestConfig.TEST_USER_PREFIX}${userId}"
    }

}
