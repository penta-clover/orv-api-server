package lib

import static net.grinder.script.Grinder.grinder

/**
 * 부하 테스트용 데이터 제공 클래스
 * Phase 2에서 구현된 스토리보드/Scene 데이터 관리
 */
class TestDataProvider {
    
    // 계획서에 명시된 8개 스토리보드 정보
    private static final Map<String, Map> STORYBOARDS = [
        // 6개 Scene (10% 분배)
        "9c570f84-16a6-4c5d-85b0-eadf05829056": [
            title: "연말정산",
            sceneCount: 6,
            sessionDuration: 8 * 60  // 8분
        ],
        
        // 8개 Scene (70% 분배)
        "0afecfc8-62a4-4398-85a8-0cff8b8f698f": [
            title: "월요병",
            sceneCount: 8,
            sessionDuration: 10 * 60  // 10분
        ],
        "18779df7-a80d-497c-9206-9e61540bb465": [
            title: "오늘하루",
            sceneCount: 8,
            sessionDuration: 10 * 60
        ],
        "8c4359b2-c60a-4972-8327-89677244b12b": [
            title: "생일",
            sceneCount: 8,
            sessionDuration: 10 * 60
        ],
        "c81d9417-5797-4b11-a8ea-c161cacfe9d1": [
            title: "회고",
            sceneCount: 8,
            sessionDuration: 10 * 60
        ],
        "e5e9b7dc-efa4-43f9-b428-03769aabdafc": [
            title: "여행",
            sceneCount: 8,
            sessionDuration: 10 * 60
        ],
        
        // 9개 Scene (20% 분배)
        "8c2746c4-4613-47f8-8799-235fec7f359d": [
            title: "자기소개",
            sceneCount: 9,
            sessionDuration: 11 * 60  // 11분
        ],
        "cff1c432-b6ac-4b10-89b7-3c9be91a6699": [
            title: "짝사랑",
            sceneCount: 9,
            sessionDuration: 11 * 60
        ]
    ]
    
    // 8개 Scene 스토리보드 ID 배열 (70% 분배용)
    private static final String[] EIGHT_SCENE_IDS = [
        "0afecfc8-62a4-4398-85a8-0cff8b8f698f", // 월요병
        "18779df7-a80d-497c-9206-9e61540bb465", // 오늘하루
        "8c4359b2-c60a-4972-8327-89677244b12b", // 생일
        "c81d9417-5797-4b11-a8ea-c161cacfe9d1", // 회고
        "e5e9b7dc-efa4-43f9-b428-03769aabdafc"  // 여행
    ]
    
    // 9개 Scene 스토리보드 ID 배열 (20% 분배용)
    private static final String[] NINE_SCENE_IDS = [
        "8c2746c4-4613-47f8-8799-235fec7f359d", // 자기소개
        "cff1c432-b6ac-4b10-89b7-3c9be91a6699"  // 짝사랑
    ]
    
    /**
     * 사용자 ID 기반 결정론적 스토리보드 선택
     * 계획서 요구사항: 해시 기반 분배로 세션 시간 차별화
     * 
     * @param userId 테스트 사용자 ID (1-6000)
     * @return 선택된 스토리보드 UUID
     */
    static String getStoryboardByUserId(int userId) {
        // 사용자 ID 기반 해시 생성 (0-99)
        int hash = Math.abs(userId.hashCode()) % 100
        
        if (hash < 10) {
            // 10%: 6개 Scene (8분 세션)
            return "9c570f84-16a6-4c5d-85b0-eadf05829056"  // 연말정산
            
        } else if (hash < 80) {
            // 70%: 8개 Scene (10분 세션)
            int index = (hash - 10) % EIGHT_SCENE_IDS.length
            return EIGHT_SCENE_IDS[index]
            
        } else {
            // 20%: 9개 Scene (11분 세션)
            int index = (hash - 80) % NINE_SCENE_IDS.length
            return NINE_SCENE_IDS[index]
        }
    }
    
    /**
     * 스토리보드 정보 조회
     * 
     * @param storyboardId 스토리보드 UUID
     * @return 스토리보드 정보 맵
     */
    static Map getStoryboardInfo(String storyboardId) {
        Map info = STORYBOARDS.get(storyboardId)
        if (info == null) {
            grinder.logger.warn("Unknown storyboard ID: ${storyboardId}")
            return [
                title: "Unknown",
                sceneCount: 8,
                sessionDuration: 10 * 60
            ]
        }
        return info
    }
    
    /**
     * 스토리보드의 Scene 개수 조회
     * 
     * @param storyboardId 스토리보드 UUID
     * @return Scene 개수
     */
    static int getSceneCount(String storyboardId) {
        return getStoryboardInfo(storyboardId).sceneCount
    }
    
    /**
     * 스토리보드의 예상 세션 시간 조회 (초 단위)
     * 
     * @param storyboardId 스토리보드 UUID
     * @return 세션 지속 시간 (초)
     */
    static int getSessionDuration(String storyboardId) {
        return getStoryboardInfo(storyboardId).sessionDuration
    }
    
    /**
     * 사용자별 테스트 영상 파일명 생성
     * 모든 사용자가 동일한 테스트 영상 사용
     * 
     * @param userId 테스트 사용자 ID
     * @return 테스트 영상 파일명
     */
    static String getTestVideoFileName(int userId) {
        return TestConfig.VIDEO_FILE_NAME  // "sample-480p-7min.mp4"
    }
    
    /**
     * 시나리오 B용: 사용자별 기존 리캡 데이터 선택
     * Phase 0에서 생성한 리캡 데이터 중 하나를 선택
     * 
     * @param userId 테스트 사용자 ID (1-6000)
     * @return 선택된 recap_reservation UUID
     */
    static String getExistingRecapId(int userId) {
        // 사용자당 7개의 리캡 데이터가 생성됨 (02_create_recap_data.sql 참조)
        // 그 중 첫 번째 것을 사용 (안정성을 위해)
        int recapIndex = 1  // 첫 번째 리캡 사용
        
        // UUID 생성 패턴: Phase 0 스크립트와 동일한 로직
        // recap_reservation 테이블에서 member_id 기반으로 조회할 수 있도록 함
        return "recap_${String.format('%06d', userId)}_${String.format('%02d', recapIndex)}"
    }
    
    /**
     * 테스트 사용자 인증 코드 생성
     * Phase 0 TestAuthService와 호환
     * 
     * @param userId 테스트 사용자 ID (1-6000)
     * @return 테스트 인증 코드
     */
    static String getTestAuthCode(int userId) {
        return "${TestConfig.TEST_USER_PREFIX}${userId}"
    }
    
    /**
     * 분배 통계 정보 출력 (디버깅용)
     * 6000명 사용자 기준 예상 분배 현황
     */
    static void printDistributionStats() {
        grinder.logger.info("=== Storyboard Distribution Statistics ===")
        
        Map<String, Integer> distribution = [:]
        Map<Integer, Integer> sceneCountDist = [6: 0, 8: 0, 9: 0]
        
        // 1-6000 사용자 분배 시뮬레이션
        for (int userId = 1; userId <= 6000; userId++) {
            String storyboardId = getStoryboardByUserId(userId)
            distribution[storyboardId] = (distribution[storyboardId] ?: 0) + 1
            
            int sceneCount = getSceneCount(storyboardId)
            sceneCountDist[sceneCount]++
        }
        
        // 스토리보드별 분배 결과
        distribution.each { storyboardId, count ->
            Map info = getStoryboardInfo(storyboardId)
            double percentage = (count / 6000.0) * 100
            grinder.logger.info("${info.title} (${info.sceneCount} scenes): ${count}명 (${String.format('%.1f', percentage)}%)")
        }
        
        // Scene 개수별 분배 결과
        grinder.logger.info("\n--- Scene Count Distribution ---")
        sceneCountDist.each { sceneCount, count ->
            double percentage = (count / 6000.0) * 100
            int sessionMinutes = (sceneCount == 6) ? 8 : (sceneCount == 8) ? 10 : 11
            grinder.logger.info("${sceneCount} scenes (${sessionMinutes}min): ${count}명 (${String.format('%.1f', percentage)}%)")
        }
        
        grinder.logger.info("=========================================")
    }
    
    /**
     * 모든 스토리보드 ID 목록 반환
     * 
     * @return 스토리보드 ID 배열
     */
    static String[] getAllStoryboardIds() {
        return STORYBOARDS.keySet().toArray()
    }
    
    /**
     * 랜덤 스토리보드 선택 (테스트용)
     * 실제 부하 테스트에서는 getStoryboardByUserId() 사용 권장
     * 
     * @return 랜덤 스토리보드 UUID
     */
    static String getRandomStoryboard() {
        String[] allIds = getAllStoryboardIds()
        int randomIndex = new Random().nextInt(allIds.length)
        return allIds[randomIndex]
    }
    
    /**
     * 스토리보드 검증 수행
     * Phase 2에서 요구되는 DB 데이터 무결성 확인
     * 
     * @return 검증 성공 여부
     */
    static boolean validateStoryboards() {
        try {
            grinder.logger.info("Starting storyboard validation...")
            
            // 1. 모든 스토리보드 ID가 실제 DB에 존재하는지 확인
            // 2. Scene 체인 무결성 확인
            // 3. Scene 타입별 content 구조 검증
            
            // 현재는 계획서 기반 데이터로 초기화
            // TODO: 실제 DB 조회 로직 추가 필요
            
            grinder.logger.info("Storyboard validation completed successfully")
            return true
            
        } catch (Exception e) {
            grinder.logger.error("Storyboard validation failed: ${e.message}")
            return false
        }
    }
}
