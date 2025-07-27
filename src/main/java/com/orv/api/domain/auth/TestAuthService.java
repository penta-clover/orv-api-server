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
    private static final int MAX_TEST_USERS = 6000;
    
    @Override
    public String getAuthorizationUrl(String state) {
        // nGrinder에서는 실제 OAuth 플로우 없이 바로 callback 호출
        return "http://localhost:8080/test-oauth-callback";
    }
    
    @Override
    public SocialUserInfo getUserInfo(String code) {
        // code 형식: "test_user_1", "test_user_2", ..., "test_user_6000"
        if (!code.startsWith(TEST_USER_PREFIX)) {
            throw new IllegalArgumentException("Invalid test auth code: " + code);
        }
        
        try {
            String userIdStr = code.replace(TEST_USER_PREFIX, "");
            int userId = Integer.parseInt(userIdStr);
            
            // 사용자 ID 범위 검증 (1-6000)
            if (userId < 1 || userId > MAX_TEST_USERS) {
                throw new IllegalArgumentException("Test user ID out of range: " + userId);
            }
            
            SocialUserInfo userInfo = new SocialUserInfo();
            userInfo.setProvider("test");
            userInfo.setId("fake_social_id_" + userId);  // social_id (UNIQUE)
            userInfo.setEmail("loadtest_" + userId + "@test.com");
            userInfo.setNickname("LT" + String.format("%06d", userId));  // LT000001 형식
            
            return userInfo;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid test user ID format: " + code);
        }
    }
}
