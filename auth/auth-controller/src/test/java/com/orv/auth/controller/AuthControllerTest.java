package com.orv.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orv.auth.orchestrator.dto.ValidationResultResponse;
import com.orv.auth.orchestrator.AuthOrchestrator;
import com.orv.auth.domain.JoinForm;
import com.orv.auth.domain.Member;
import com.orv.auth.domain.SocialUserInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Mock
    private AuthOrchestrator authOrchestrator;

    @InjectMocks
    private AuthController authController;

    private String callbackUrl = "http://localhost:3000/callback";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        ReflectionTestUtils.setField(authController, "callbackUrl", callbackUrl);
        ReflectionTestUtils.setField(authController, "sessionCookieName", "ORVSESSION");
        ReflectionTestUtils.setField(authController, "secureSessionCookie", true);
        ReflectionTestUtils.setField(authController, "sessionCookieSameSite", "Lax");
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testLoginRedirect() throws Exception {
        //given
        String provider = "kakao";
        String expectedAuthUrl = "https://kauth.kakao.com/oauth/authorize?state=testState";

        when(authOrchestrator.getAuthorizationUrl(eq(provider), anyString())).thenReturn(expectedAuthUrl);

        // when & then
        mockMvc.perform(get("/api/v0/auth/login/{provider}", provider))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedAuthUrl));
    }

    @Test
    public void testCallback_whenMemberExists() throws Exception {
        // given
        String provider = "kakao";
        String code = "dummyCode";
        String state = "testState";

        SocialUserInfo socialUserInfo = new SocialUserInfo();
        socialUserInfo.setProvider(provider);
        socialUserInfo.setId("12345");

        Member existingMember = new Member();
        existingMember.setId(UUID.randomUUID());
        existingMember.setProvider(provider);
        existingMember.setSocialId("12345");

        MockHttpSession session = oauthSession(provider, state);

        when(authOrchestrator.getUserInfo(provider, code)).thenReturn(socialUserInfo);
        when(authOrchestrator.findByProviderAndSocialId(provider, "12345")).thenReturn(Optional.of(existingMember));
        when(authOrchestrator.findRolesById(existingMember.getId())).thenReturn(Optional.of(Collections.emptyList()));

        // 가입된 유저일 경우, isNewUser는 false
        String expectedRedirectUrl = callbackUrl + "?isNewUser=false";

        // when & then
        mockMvc.perform(get("/api/v0/auth/callback/{provider}", provider)
                        .session(session)
                        .param("code", code)
                        .param("state", state))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedRedirectUrl));

        assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNotNull();
    }


    @Test
    public void testCallback_whenMemberNotExists() throws Exception {
        // given
        String provider = "kakao";
        String code = "dummyCode";
        String state = "testState";

        SocialUserInfo socialUserInfo = new SocialUserInfo();
        socialUserInfo.setProvider(provider);
        socialUserInfo.setId("12345");

        // 미가입 유저로 처리 (Optional.empty())
        when(authOrchestrator.getUserInfo(provider, code)).thenReturn(socialUserInfo);
        when(authOrchestrator.findByProviderAndSocialId(provider, "12345")).thenReturn(Optional.empty());

        MockHttpSession session = oauthSession(provider, state);

        // 가입되지 않은 경우, isNewUser는 true
        String expectedRedirectUrl = callbackUrl + "?isNewUser=true";

        // when & then
        mockMvc.perform(get("/api/v0/auth/callback/{provider}", provider)
                        .session(session)
                        .param("code", code)
                        .param("state", state))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedRedirectUrl));

        assertThat(session.getAttribute(AuthController.PENDING_SIGNUP_MEMBER_ID_SESSION_ATTRIBUTE)).isNotNull();
        assertThat(session.getAttribute(AuthController.PENDING_SIGNUP_PROVIDER_SESSION_ATTRIBUTE)).isEqualTo(provider);
        assertThat(session.getAttribute(AuthController.PENDING_SIGNUP_SOCIAL_ID_SESSION_ATTRIBUTE)).isEqualTo("12345");
    }

    @Test
    public void testValidNickname_whenNicknameValid() throws Exception {
        // given
        String nickname = "abc가나123";
        ValidationResultResponse validationResult = new ValidationResultResponse();
        validationResult.setNickname(nickname);
        validationResult.setIsValid(true);
        validationResult.setIsExists(false);

        when(authOrchestrator.validateNickname(nickname)).thenReturn(validationResult);

        // when
        mockMvc.perform(get("/api/v0/auth/nicknames").param("nickname", nickname))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.isValid").value(true))
                .andExpect(jsonPath("$.data.isExists").value(false));
    }


    @Test
    public void testJoinEndpoint() throws Exception {
        // given: 요청에 사용할 JoinForm 데이터
        JoinForm joinForm = new JoinForm();
        joinForm.setNickname("testNick");
        joinForm.setGender("MALE");
        joinForm.setBirthDay(LocalDate.of(2002, 5, 31));

        String pendingMemberId = UUID.randomUUID().toString();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthController.PENDING_SIGNUP_MEMBER_ID_SESSION_ATTRIBUTE, pendingMemberId);
        session.setAttribute(AuthController.PENDING_SIGNUP_PROVIDER_SESSION_ATTRIBUTE, "testProvider");
        session.setAttribute(AuthController.PENDING_SIGNUP_SOCIAL_ID_SESSION_ATTRIBUTE, "testSocialId");

        // authOrchestrator.join() 호출시 아무 동작 없음 (void)
        Mockito.doNothing().when(authOrchestrator).join(anyString(), anyString(), anyString(), any(), anyString(), anyString(), any());

        // when & then
        mockMvc.perform(post("/api/v0/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session)
                        .content(objectMapper.writeValueAsString(joinForm)))
                .andExpect(status().isOk())
                // ApiResponse의 결과가 null로 반환되지만, 성공 코드 200을 전달한다고 가정
                .andExpect(jsonPath("$.statusCode", equalTo("200")))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(authOrchestrator).join(eq(pendingMemberId), eq("testNick"), eq("MALE"), eq(LocalDate.of(2002, 5, 31)), eq("testProvider"), eq("testSocialId"), any());
    }

    @Test
    public void testJoinEndpoint_withoutPendingSession_returnsUnauthorized() throws Exception {
        JoinForm joinForm = new JoinForm();
        joinForm.setNickname("testNick");
        joinForm.setGender("MALE");
        joinForm.setBirthDay(LocalDate.of(2002, 5, 31));

        mockMvc.perform(post("/api/v0/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinForm)))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession oauthSession(String provider, String state) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthController.OAUTH_STATE_SESSION_ATTRIBUTE, state);
        session.setAttribute(AuthController.OAUTH_PROVIDER_SESSION_ATTRIBUTE, provider);
        return session;
    }
}
