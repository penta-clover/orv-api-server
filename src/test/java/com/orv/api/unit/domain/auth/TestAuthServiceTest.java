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
        @ValueSource(strings = {
                "test_user_0_0_0", "test_user_0_0_1", "test_user_1_2_3",
                "test_user_5_10_100", "test_user_999_999_999"
        })
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
        @ValueSource(strings = {
                // 기존 단순 숫자 형식 (더 이상 지원 안함)
                "test_user_1", "test_user_1000", "test_user_6000",
                // 형식 오류
                "test_user_0", "test_user", "test", "invalid_format",
                "90C1AD20-288B-467B-82D5-7E4A0F6CFF60",
                // 음수
                "test_user_-1_0_0", "test_user_0_-1_0", "test_user_0_0_-1",
                // 잘못된 구분자/개수
                "test_user_1-2-3", "test_user_1.2.3", "test_user_1_2_3_4", "test_user_1_2",
                // 숫자가 아닌 값
                "test_user_a_b_c", "test_user_1_a_3", "test_user_a_2_3"
        })
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
