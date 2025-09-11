package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.SocialUserInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 부하테스트 전용 인증 서비스
 * test.auth.enabled=true 일 때만 활성화되며, loadtest 또는 test 프로파일에서만 동작
 * 단, 현재 별도의 부하 테스트 환경이 없으므로 임시로 production을 통해 테스트함.
 */
@Service
// @ConditionalOnProperty(name = "test.auth.enabled", havingValue = "true")
// @Profile({"loadtest", "test"})
public class TestAuthService implements SocialAuthService {
    
    private static final String TEST_USER_PREFIX = "test_user_";
    
    @Override
    public String getAuthorizationUrl(String state) {
        // nGrinder에서는 실제 OAuth 플로우 없이 바로 callback 호출
        return "http://localhost:8080/test-oauth-callback";
    }
    
    @Override
    public SocialUserInfo getUserInfo(String code) {
        // code 형식: "test_user_0_0_1", "test_user_1_2_3" (agent_process_thread 형식만 지원)
        if (!code.startsWith(TEST_USER_PREFIX)) {
            throw new IllegalArgumentException("Invalid test auth code: " + code);
        }
        
        try {
            String userIdStr = code.replace(TEST_USER_PREFIX, "");
            String[] parts = userIdStr.split("_");
            
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid test user ID format. Expected: test_user_{agent}_{process}_{thread}");
            }
            
            // agent_process_thread 형식 파싱
            int agentNumber = Integer.parseInt(parts[0]);
            int processNumber = Integer.parseInt(parts[1]);
            int threadNumber = Integer.parseInt(parts[2]);
            
            // 음수 검증
            if (agentNumber < 0 || processNumber < 0 || threadNumber < 0) {
                throw new IllegalArgumentException("Agent, process, thread numbers must be non-negative");
            }
            
            String uniqueId = userIdStr; // "0_0_1"
            
            SocialUserInfo userInfo = new SocialUserInfo();
            userInfo.setProvider("test");
            userInfo.setId("fake_social_id_" + uniqueId);  // social_id (UNIQUE)
            userInfo.setEmail("loadtest_" + uniqueId.replace("_", "-") + "@test.com");
            userInfo.setNickname("LT_" + uniqueId.replace("_", "-"));
            
            return userInfo;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid test user ID format: " + code);
        }
    }
}
