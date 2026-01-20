package com.orv.api.unit.domain.health;

import com.orv.api.domain.health.controller.HealthController;
import com.orv.api.domain.health.controller.dto.HealthStatusResponse;
import com.orv.api.domain.health.orchestrator.HealthOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthOrchestrator healthOrchestrator;

    @Test
    public void getHealth_returnsHealthyStatus() throws Exception {
        // given
        when(healthOrchestrator.getHealthStatus())
                .thenReturn(new HealthStatusResponse("this server is healthy"));

        // when & then
        mockMvc.perform(get("/api/v0/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("this server is healthy"));
    }
}