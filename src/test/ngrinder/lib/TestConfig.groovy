package lib

/**
 * nGrinder 부하테스트 환경 설정
 * Phase 1에서 구현된 기본 설정 클래스
 */
class TestConfig {
    // 서버 환경 설정
    static final String BASE_URL = "http://localhost:8080"
    static final String API_BASE_PATH = "/api/v0"
    
    // 테스트 사용자 설정 (Phase 0에서 생성한 사용자 기반)
    static final int TOTAL_TEST_USERS = 6000
    static final String TEST_USER_PREFIX = "test_user_"
    
    // 부하 패턴 설정 (계획서 기준)
    static final int TARGET_VU = 86              // 목표 동시 사용자 수
    static final int SPIKE_VU = 258              // 스파이크 테스트 사용자 수 (3배)
    static final int RAMP_UP_DURATION = 10 * 60  // 10분 (초 단위)
    static final int PEAK_DURATION = 10 * 60     // 10분 (초 단위)
    static final int SPIKE_DURATION = 5 * 60     // 5분 (초 단위)
    
    // 성능 목표 지표
    static final int MAX_RESPONSE_TIME = 500     // 최대 응답시간 (ms)
    static final double MAX_ERROR_RATE = 0.001   // 최대 에러율 (0.1%)
    
    // 시나리오별 분배 비율
    static final double SCENARIO_A_RATIO = 0.5   // 시나리오 A: 50%
    static final double SCENARIO_B_RATIO = 0.5   // 시나리오 B: 50%
    
    // 시나리오 A 설정 (비디오 업로드 → 리캡 생성)
    static final int SCENARIO_A_SESSION_MIN = 8 * 60   // 8분 (6개 Scene)
    static final int SCENARIO_A_SESSION_MID = 10 * 60  // 10분 (8개 Scene)
    static final int SCENARIO_A_SESSION_MAX = 11 * 60  // 11분 (9개 Scene)
    static final int SCENE_DELAY_SECONDS = 50          // Scene간 50초 딜레이
    
    // 시나리오 B 설정 (리캡 조회 → 오디오 스트리밍)
    static final int SCENARIO_B_SESSION_DURATION = 7 * 60  // 7분 세션
    static final int AUDIO_STREAMING_DURATION = 7 * 60     // 7분 스트리밍
    
    // JWT 토큰 설정
    static final int JWT_EXPIRATION_BUFFER = 60    // 토큰 만료 1분 전 재인증
    
    // 파일 업로드 설정
    static final String VIDEO_FILE_NAME = "sample-480p-7min.mp4"
    static final int VIDEO_FILE_SIZE_MB = 5        // 약 5MB
    static final int UPLOAD_TIMEOUT_MS = 30000     // 30초 타임아웃
    
    // 오디오 스트리밍 설정
    static final int AUDIO_CHUNK_SIZE = 1024 * 64  // 64KB 청크
    static final int STREAMING_INTERVAL_MS = 100   // 100ms 간격
    
    // HTTP 연결 설정
    static final int CONNECTION_TIMEOUT_MS = 10000 // 10초
    static final int READ_TIMEOUT_MS = 30000       // 30초
    static final int MAX_RETRY_COUNT = 3           // 최대 재시도 횟수
}
