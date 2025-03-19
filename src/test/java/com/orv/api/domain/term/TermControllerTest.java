package com.orv.api.domain.term;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orv.api.domain.term.dto.TermAgreementForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TermController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc(addFilters = false)
public class TermControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TermRepository termRepository;

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testCreateAgreement() throws Exception {
        // given
        TermAgreementForm termAgreementForm = new TermAgreementForm();
        termAgreementForm.setTerm("privacy250301");
        termAgreementForm.setValue("Y");

        String agreementId = UUID.randomUUID().toString();
        when(termRepository.saveAgreement(any(), any(), any(), any(), any())).thenReturn(Optional.of(agreementId));

        // when
        mockMvc.perform(post("/api/v0/term/agreement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(termAgreementForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(agreementId))
                .andDo(
                        document("term/add-agreement",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestFields(
                                        fieldWithPath("term").description("약관 이름"),
                                        fieldWithPath("value").description("동의 여부")
                                ),
                                responseFields(
                                        fieldWithPath("statusCode").description("응답 상태 코드"),
                                        fieldWithPath("message").description("응답 상태 메시지"),
                                        fieldWithPath("data").description("약관 동의 ID")
                                )
                        )
                );
    }
}
