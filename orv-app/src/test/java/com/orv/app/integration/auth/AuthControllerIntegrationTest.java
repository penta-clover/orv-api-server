package com.orv.app.integration.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orv.auth.controller.AuthController;
import com.orv.auth.repository.MemberRepository;
import com.orv.auth.service.JwtTokenService;
import com.orv.auth.domain.JoinForm;
import com.orv.auth.domain.Member;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenService jwtTokenProvider;

    @AfterEach
    void tearDown() {

    }

    @Test
    void joinEndpoint_insertsMemberIntoDb() throws Exception {

        // given
        JoinForm joinForm = new JoinForm();
        joinForm.setNickname("testNick");
        joinForm.setGender("MALE");
        joinForm.setBirthDay(LocalDate.of(2002, 5, 31));

        String dummyToken = jwtTokenProvider.createToken(UUID.randomUUID().toString(), Map.of(
                "provider", "google",
                "socialId", "google-social-id"
        ));

        String bearerToken = "Bearer " + dummyToken;

        // when: POST /api/v0/auth/join 엔드포인트에 요청 전송
        MvcResult mvcResult = mockMvc.perform(post("/api/v0/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearerToken)
                        .content(objectMapper.writeValueAsString(joinForm)))
                .andExpect(status().isOk())
                .andReturn();

        // then: memberRepository.findByNickname()를 통해 DB에 회원이 저장되었는지 확인
        Optional<Member> optionalMember = memberRepository.findByNickname("testNick");
        assertThat(optionalMember).isPresent();
        Member member = optionalMember.get();
        assertThat(member.getNickname()).isEqualTo("testNick");
        assertThat(member.getGender()).isEqualTo("MALE");
        assertThat(member.getBirthday()).isEqualTo(LocalDate.of(2002, 5, 31));
        // JWT 페이로드에서 추출한 provider, socialId가 제대로 저장되었는지 검증
        assertThat(member.getProvider()).isEqualTo("google");
        assertThat(member.getSocialId()).isEqualTo("google-social-id");

    }
}
