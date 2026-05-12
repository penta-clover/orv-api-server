package com.orv.app.integration.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orv.auth.controller.AuthController;
import com.orv.auth.repository.MemberRepository;
import com.orv.auth.domain.JoinForm;
import com.orv.auth.domain.Member;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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

        String pendingMemberId = UUID.randomUUID().toString();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthController.PENDING_SIGNUP_MEMBER_ID_SESSION_ATTRIBUTE, pendingMemberId);
        session.setAttribute(AuthController.PENDING_SIGNUP_PROVIDER_SESSION_ATTRIBUTE, "google");
        session.setAttribute(AuthController.PENDING_SIGNUP_SOCIAL_ID_SESSION_ATTRIBUTE, "google-social-id");

        // when: POST /api/v0/auth/join 엔드포인트에 요청 전송
        MvcResult mvcResult = mockMvc.perform(post("/api/v0/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", "http://localhost:3000")
                        .session(session)
                        .content(objectMapper.writeValueAsString(joinForm)))
                .andExpect(status().isOk())
                .andReturn();

        // then: memberRepository.findByNickname()를 통해 DB에 회원이 저장되었는지 확인
        Optional<Member> optionalMember = memberRepository.findByNickname("testNick");
        assertThat(optionalMember).isPresent();
        Member member = optionalMember.get();
        assertThat(member.getId()).isEqualTo(UUID.fromString(pendingMemberId));
        assertThat(member.getNickname()).isEqualTo("testNick");
        assertThat(member.getGender()).isEqualTo("MALE");
        assertThat(member.getBirthday()).isEqualTo(LocalDate.of(2002, 5, 31));
        // JWT 페이로드에서 추출한 provider, socialId가 제대로 저장되었는지 검증
        assertThat(member.getProvider()).isEqualTo("google");
        assertThat(member.getSocialId()).isEqualTo("google-social-id");

    }
}
