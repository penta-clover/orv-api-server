package com.orv.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orv.auth.orchestrator.dto.MemberInfoResponse;
import com.orv.auth.orchestrator.MemberOrchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class MemberControllerTest {
    private MockMvc mockMvc;

    @Mock
    private MemberOrchestrator memberOrchestrator;

    @InjectMocks
    private MemberController memberController;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("054c3e8a-3387-4eb3-ac8a-31a48221f192", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    public void testGetMyInfo() throws Exception {
        // given
        MemberInfoResponse myInfo = new MemberInfoResponse();
        myInfo.setId(UUID.fromString("054c3e8a-3387-4eb3-ac8a-31a48221f192"));
        myInfo.setNickname("현준");
        myInfo.setProfileImageUrl("https://www.naver.com/favicon.ico");
        myInfo.setCreatedAt(LocalDateTime.now());

        when(memberOrchestrator.getMyInfo(any())).thenReturn(myInfo);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/member/my-info"));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(myInfo.getId().toString()))
                .andExpect(jsonPath("$.data.nickname").value(myInfo.getNickname()))
                .andExpect(jsonPath("$.data.profileImageUrl").value(myInfo.getProfileImageUrl()))
                .andExpect(jsonPath("$.data.createdAt").value(org.hamcrest.Matchers.startsWith(myInfo.getCreatedAt().truncatedTo(ChronoUnit.SECONDS).toString())));
    }
}
