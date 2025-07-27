package com.orv.api.unit.domain.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.orv.api.domain.auth.TestAuthService;
import com.orv.api.domain.auth.dto.SocialUserInfo;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

class TestAuthServiceTest {
    private TestAuthService testAuthService;

    @BeforeEach
    void setUp() {
        testAuthService = new TestAuthService();
    }

    @Nested
    @DisplayName("getAuthorizationUrl 메서드는")
    class getAuthorizationUrl {
        @Test
        @DisplayName("authorization url을 반환한다")
        void shouldReturnTestAuthorizationUrl() {
            // given
            String state = "4B56D51C-935A-4EEA-A4F7-FA57DBC13D20";

            // when
            String authorizationUrl = testAuthService.getAuthorizationUrl(state);

            // then
            assertThat(authorizationUrl).isNotBlank();
        }
    }

    @Nested
    @DisplayName("getUserInfo 메서드는")
    class GetUserInfo {
        @ParameterizedTest
        @ValueSource(strings = { "test_user_1", "test_user_2", "test_user_1000", "test_user_5999", "test_user_6000" })
        @DisplayName("유효한 authorization code가 주어졌을 때 테스트 사용자 정보를 반환한다.")
        void shouldReturnTestUserInfoWhenValidCode(String authorizationCode) {
            // given
            
            // when
            SocialUserInfo socialUserInfo = testAuthService.getUserInfo(authorizationCode);
            
            // then
            assertThat(socialUserInfo).isNotNull();
            assertThat(socialUserInfo.getProvider()).isEqualTo("test");
            assertThat(socialUserInfo.getNickname()).isNotBlank();
            assertThat(socialUserInfo.getEmail()).isNotBlank();
            assertThat(socialUserInfo.getId()).isNotBlank();
        }

        @ParameterizedTest
        @ValueSource(strings = { "test_user_0", "90C1AD20-288B-467B-82D5-7E4A0F6CFF60", "test_user", "test", "test_user_6001", "test_user_9876543210" })
        @DisplayName("잘못된 authorization code가 주어졌을 때 예외를 발생시킨다.")
        void shouldThrowWhenInvalidCode(String wrongAuthorizationCode) {
            // given
            
            // when
            Throwable thrownException = catchThrowable(() -> testAuthService.getUserInfo(wrongAuthorizationCode));
            
            // then
            assertThat(thrownException).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
