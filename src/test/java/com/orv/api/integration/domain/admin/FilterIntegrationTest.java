package com.orv.api.integration.domain.admin;

import com.orv.api.JwtTokenServiceTestConfig;
import com.orv.api.domain.admin.controller.AdminController;
import com.orv.api.domain.auth.service.JwtTokenService;
import com.orv.api.global.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc(addFilters = true)
@Import(JwtTokenServiceTestConfig.class)
public class FilterIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenProvider;

    @MockitoBean
    private AdminController adminController;


    @Test
    public void testAdminAccess_whenNotAdmin_thenReturnForbidden() throws Exception {
        // given
        String token = jwtTokenProvider.createToken("054c3e8a-3387-4eb3-ac8a-31a48221f192", Map.of("provider", "kakao", "socialId", "dummy-social-id", "roles", Collections.emptyList()));
        when(adminController.checkAdmin()).thenReturn(ApiResponse.success("You are an admin!", 200));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/admin/")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192", roles = "ADMIN")
    public void testAdminAccess_whenIsAdmin_thenReturnData() throws Exception {
        // given
        String token = jwtTokenProvider.createToken("054c3e8a-3387-4eb3-ac8a-31a48221f192", Map.of("provider", "kakao", "socialId", "dummy-social-id", "roles", List.of("ADMIN")));
        when(adminController.checkAdmin()).thenReturn(ApiResponse.success("You are an admin!", 200));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/admin/")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isOk());
    }
}
