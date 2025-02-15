package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.Member;
import com.orv.api.domain.auth.dto.SocialUserInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    // AuthController가 의존하는 빈들을 모킹합니다.
    @MockitoBean
    private SocialAuthServiceFactory socialAuthServiceFactory;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // SocialAuthService도 모킹 (컨트롤러 내부에서 사용됨)
    @MockitoBean
    private SocialAuthService socialAuthService;

    @Value("${security.frontend.callback-url}")
    private String callbackUrl;

    @Test
    public void testLoginRedirect() throws Exception {
        //given
        String provider = "kakao";
        String expectedAuthUrl = "http://kauth.kakao.com/oauth/authorize?state=testState";

        when(socialAuthService.getAuthorizationUrl(anyString())).thenReturn(expectedAuthUrl);
        when(socialAuthServiceFactory.getSocialAuthService(provider)).thenReturn(socialAuthService);

        // when & then
        mockMvc.perform(get("/api/v0/auth/login/" + provider))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedAuthUrl));
    }

    @Test
    public void testCallback_whenMemberExists() throws Exception {
        // given
        String provider = "kakao";
        String code = "dummyCode";

        SocialUserInfo socialUserInfo = new SocialUserInfo();
        socialUserInfo.setProvider(provider);
        socialUserInfo.setId("12345");

        Member existingMember = new Member();
        existingMember.setId("memberId123");
        existingMember.setProvider(provider);
        existingMember.setSocialId("12345");

        String token = "dummyJwtToken";

        when(socialAuthServiceFactory.getSocialAuthService(provider)).thenReturn(socialAuthService);
        when(socialAuthService.getUserInfo(code)).thenReturn(socialUserInfo);
        when(memberRepository.findByProviderAndSocialId(provider, "12345")).thenReturn(Optional.of(existingMember));
        when(jwtTokenProvider.createToken(eq(existingMember.getId()), any(Map.class))).thenReturn(token);

        // 가입된 유저일 경우, isNewUser는 false
        String expectedRedirectUrl = callbackUrl + "?isNewUser=false&jwtToken=" + token;

        // when & then
        mockMvc.perform(get("/api/v0/auth/callback/" + provider)
                        .param("code", code))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedRedirectUrl));
    }


    @Test
    public void testCallback_whenMemberNotExists() throws Exception {
        // given
        String provider = "kakao";
        String code = "dummyCode";

        SocialUserInfo socialUserInfo = new SocialUserInfo();
        socialUserInfo.setProvider(provider);
        socialUserInfo.setId("12345");

        // 미가입 유저로 처리 (Optional.empty())
        when(socialAuthServiceFactory.getSocialAuthService(provider)).thenReturn(socialAuthService);
        when(socialAuthService.getUserInfo(code)).thenReturn(socialUserInfo);
        when(memberRepository.findByProviderAndSocialId(provider, "12345")).thenReturn(Optional.empty());

        // 미가입 유저일 경우, 임시 ID가 생성되지만 테스트에서는 그 값을 신경쓰지 않고 토큰만 모킹
        String token = "dummyJwtTokenNew";
        when(jwtTokenProvider.createToken(anyString(), any(Map.class))).thenReturn(token);

        // 가입되지 않은 경우, isNewUser는 true
        String expectedRedirectUrl = callbackUrl + "?isNewUser=true&jwtToken=" + token;

        // when & then
        mockMvc.perform(get("/api/v0/auth/callback/" + provider)
                        .param("code", code))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedRedirectUrl));
    }
}
