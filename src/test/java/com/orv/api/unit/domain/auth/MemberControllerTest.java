package com.orv.api.unit.domain.auth;


import com.orv.api.domain.auth.MemberController;
import com.orv.api.domain.auth.MemberService;
import com.orv.api.domain.auth.dto.MemberInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc(addFilters = false)
public class MemberControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testGetMyInfo() throws Exception {
        // given
        MemberInfo myInfo = new MemberInfo();
        myInfo.setId(UUID.fromString("054c3e8a-3387-4eb3-ac8a-31a48221f192"));
        myInfo.setNickname("현준");
        myInfo.setProfileImageUrl("https://www.naver.com/favicon.ico");
        myInfo.setCreatedAt(LocalDateTime.now());

        when(memberService.getMyInfo(any())).thenReturn(myInfo);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/member/my-info"));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(myInfo.getId().toString()))
                .andExpect(jsonPath("$.data.nickname").value(myInfo.getNickname()))
                .andExpect(jsonPath("$.data.profileImageUrl").value(myInfo.getProfileImageUrl()))
                .andExpect(jsonPath("$.data.createdAt").value(myInfo.getCreatedAt().toString()))
                .andDo(document("member/get-my-info",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드"),
                                fieldWithPath("message").description("응답 상태 메시지"),
                                fieldWithPath("data.id").description("(DB 상에 저장된) 나의 id"),
                                fieldWithPath("data.nickname").description("나의 닉네임"),
                                fieldWithPath("data.profileImageUrl").description("나의 프로필 이미지 주소"),
                                fieldWithPath("data.createdAt").description("서비스 가입 일시")
                        )
                ));
    }
}
