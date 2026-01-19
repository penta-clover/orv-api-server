package com.orv.api.unit.domain.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orv.api.domain.auth.service.MemberService;
import com.orv.api.domain.auth.controller.AuthController;
import com.orv.api.domain.auth.repository.MemberRepository;
import com.orv.api.domain.auth.service.JwtTokenService;
import com.orv.api.domain.auth.service.SocialAuthService;
import com.orv.api.domain.auth.service.SocialAuthServiceResolver;
import com.orv.api.domain.auth.service.dto.JoinForm;
import com.orv.api.domain.auth.service.dto.Member;
import com.orv.api.domain.auth.service.dto.SocialUserInfo;
import com.orv.api.domain.auth.service.dto.ValidationResult;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;

@WebMvcTest(AuthController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // JSON 변환용

    // AuthController가 의존하는 빈들을 모킹합니다.
    @MockitoBean
    private SocialAuthServiceResolver socialAuthServiceFactory;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private JwtTokenService jwtTokenProvider;

    // SocialAuthService도 모킹 (컨트롤러 내부에서 사용됨)
    @MockitoBean
    private SocialAuthService socialAuthService;

    @Value("${security.frontend.callback-url}")
    private String callbackUrl;

    @Test
    public void testLoginRedirect() throws Exception {
        //given
        String provider = "kakao";
        String expectedAuthUrl = "https://kauth.kakao.com/oauth/authorize?state=testState";

        when(socialAuthService.getAuthorizationUrl(anyString())).thenReturn(expectedAuthUrl);
        when(socialAuthServiceFactory.getSocialAuthService(provider)).thenReturn(socialAuthService);

        // when & then
        mockMvc.perform(get("/api/v0/auth/login/{provider}", provider))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedAuthUrl))
                .andDo(document("auth/login-redirect",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        RequestDocumentation.pathParameters(
                                RequestDocumentation.parameterWithName("provider").description("소셜 로그인 공급자 (예: kakao)")
                        )
                ));
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
        existingMember.setId(UUID.randomUUID());
        existingMember.setProvider(provider);
        existingMember.setSocialId("12345");

        String token = "dummyJwtToken";

        when(socialAuthServiceFactory.getSocialAuthService(provider)).thenReturn(socialAuthService);
        when(socialAuthService.getUserInfo(code)).thenReturn(socialUserInfo);
        when(memberRepository.findByProviderAndSocialId(provider, "12345")).thenReturn(Optional.of(existingMember));
        when(memberRepository.findRolesById(existingMember.getId())).thenReturn(Optional.of(Collections.emptyList()));
        when(jwtTokenProvider.createToken(eq(existingMember.getId().toString()), any(Map.class))).thenReturn(token);

        // 가입된 유저일 경우, isNewUser는 false
        String expectedRedirectUrl = callbackUrl + "?isNewUser=false&jwtToken=" + token;

        // when & then
        mockMvc.perform(get("/api/v0/auth/callback/{provider}", provider)
                        .param("code", code))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedRedirectUrl))
                .andDo(document("auth/callback-existing-user",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        RequestDocumentation.pathParameters(
                                RequestDocumentation.parameterWithName("provider").description("소셜 로그인 공급자 (예: kakao)")
                        ),
                        RequestDocumentation.queryParameters(
                                RequestDocumentation.parameterWithName("code").description("소셜 로그인 인증 코드")
                        )
                ));
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
        mockMvc.perform(get("/api/v0/auth/callback/{provider}", provider)
                        .param("code", code))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedRedirectUrl))
                .andDo(document("auth/callback-new-user",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        RequestDocumentation.pathParameters(
                                RequestDocumentation.parameterWithName("provider").description("소셜 로그인 공급자 (예: kakao)")
                        ),
                        RequestDocumentation.queryParameters(
                                RequestDocumentation.parameterWithName("code").description("소셜 로그인 인증 코드")
                        )
                ));
    }

    @Test
    public void testValidNickname_whenNicknameValid() throws Exception {
        // given
        String nickname = "abc가나123";
        ValidationResult validationResult = new ValidationResult();
        validationResult.setNickname(nickname);
        validationResult.setIsValid(true);
        validationResult.setIsExists(false);

        when(memberService.validateNickname(nickname)).thenReturn(validationResult);

        // when
        mockMvc.perform(get("/api/v0/auth/nicknames").param("nickname", nickname))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.isValid").value(true))
                .andExpect(jsonPath("$.data.isExists").value(false))
                .andDo(document("auth/validate-nickname",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        RequestDocumentation.queryParameters(
                                RequestDocumentation.parameterWithName("nickname").description("검증할 닉네임")
                        ),
                        PayloadDocumentation.responseFields(
                                PayloadDocumentation.fieldWithPath("statusCode").description("응답 상태 코드"),
                                PayloadDocumentation.fieldWithPath("message").description("응답 상태 메시지"),
                                PayloadDocumentation.fieldWithPath("data.nickname").description("입력된 닉네임"),
                                PayloadDocumentation.fieldWithPath("data.isValid").description("닉네임 유효성 여부"),
                                PayloadDocumentation.fieldWithPath("data.isExists").description("닉네임 중복 여부")
                        )
                ));
    }


    @Test
    @WithMockUser(username = "1fae8d62-fdfb-47b2-a91d-182bec52ef47")
    public void testJoinEndpoint() throws Exception {
        // given: 요청에 사용할 JoinForm 데이터
        JoinForm joinForm = new JoinForm();
        joinForm.setNickname("testNick");
        joinForm.setGender("MALE");
        joinForm.setBirthDay(LocalDate.of(2002, 5, 31));

        // Authorization 헤더에서 추출할 토큰과 페이로드 설정
        String token = "dummyToken";
        String bearerToken = "Bearer " + token;
        Map<String, Object> payload = Map.of(
                "id", UUID.randomUUID().toString(),
                "provider", "testProvider",
                "socialId", "testSocialId"
        );

        // jwtTokenProvider.getPayload() 모킹
        Mockito.when(jwtTokenProvider.getPayload(eq(token))).thenReturn(payload);
        // memberService.join() 호출시 성공했다고 가정
        Mockito.when(memberService.join(anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(true);

        // when & then
        mockMvc.perform(post("/api/v0/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearerToken)
                        .content(objectMapper.writeValueAsString(joinForm)))
                .andExpect(status().isOk())
                // ApiResponse의 결과가 null로 반환되지만, 성공 코드 200을 전달한다고 가정
                .andExpect(jsonPath("$.statusCode", equalTo("200")))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andDo(document("auth/join",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("JWT 토큰 (Bearer 타입)")
                        ),
                        PayloadDocumentation.requestFields(
                                PayloadDocumentation.fieldWithPath("nickname").description("회원 가입 시 사용할 닉네임"),
                                PayloadDocumentation.fieldWithPath("gender").description("회원 성별 (예: MALE, FEMALE)"),
                                PayloadDocumentation.fieldWithPath("birthDay").description("생년월일 (YYYY-MM-DD 형식)"),
                                PayloadDocumentation.fieldWithPath("phoneNumber").description("전화번호 (하이픈X)")
                        ),
                        PayloadDocumentation.responseFields(
                                PayloadDocumentation.fieldWithPath("statusCode").description("응답 상태 코드"),
                                PayloadDocumentation.fieldWithPath("message").description("응답 상태 메시지"),
                                PayloadDocumentation.fieldWithPath("data").description("null")
                                // data가 없거나 null인 경우 생략 가능
                        )
                ));
    }
}
