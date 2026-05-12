package com.orv.app.integration.admin;

import com.orv.app.config.SecurityConfig;
import com.orv.admin.controller.AdminController;
import com.orv.admin.orchestrator.AdminOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc(addFilters = true)
@ContextConfiguration(classes = {AdminController.class, SecurityConfig.class})
@TestPropertySource(properties = "security.cors.allowed-origins=http://localhost:3000")
public class FilterIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminOrchestrator adminOrchestrator;


    @Test
    public void testAdminAccess_whenNotAdmin_thenReturnForbidden() throws Exception {
        // when
        ResultActions resultActions = mockMvc.perform(get("/api/admin/")
                .with(user("054c3e8a-3387-4eb3-ac8a-31a48221f192"))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    public void testAdminAccess_whenIsAdmin_thenReturnData() throws Exception {
        // when
        ResultActions resultActions = mockMvc.perform(get("/api/admin/")
                .with(user("054c3e8a-3387-4eb3-ac8a-31a48221f192").authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isOk());
    }

    @Test
    public void testAdminAccess_whenOnlyBearerToken_thenReturnUnauthorized() throws Exception {
        // when
        ResultActions resultActions = mockMvc.perform(get("/api/admin/")
                .header("Authorization", "Bearer legacy-token")
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isUnauthorized());
    }
}
