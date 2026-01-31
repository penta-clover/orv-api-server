package com.orv.term.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orv.term.controller.TermController;
import com.orv.term.controller.dto.TermAgreementRequest;
import com.orv.term.orchestrator.TermOrchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class TermControllerTest {
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Mock
    private TermOrchestrator termOrchestrator;

    @InjectMocks
    private TermController termController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(termController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("054c3e8a-3387-4eb3-ac8a-31a48221f192", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    public void testCreateAgreement() throws Exception {
        // given
        TermAgreementRequest termAgreementRequest = new TermAgreementRequest();
        termAgreementRequest.setTerm("privacy250301");
        termAgreementRequest.setValue("Y");

        String agreementId = UUID.randomUUID().toString();
        when(termOrchestrator.createAgreement(any(UUID.class), any(String.class), any(String.class), any(String.class))).thenReturn(Optional.of(agreementId));

        // when
        mockMvc.perform(post("/api/v0/term/agreement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(termAgreementRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(agreementId));
    }
}
