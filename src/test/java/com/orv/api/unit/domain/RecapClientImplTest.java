package com.orv.api.unit.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orv.api.domain.reservation.dto.InterviewScenario;
import com.orv.api.domain.reservation.dto.RecapContent;
import com.orv.api.domain.reservation.dto.RecapServerRequest;
import com.orv.api.domain.reservation.dto.RecapServerResponse;
import com.orv.api.infra.recap.RecapClientImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(RecapClientImpl.class)
class RecapClientImplTest {

    @Autowired
    private RecapClientImpl recapClient;

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private ObjectMapper objectMapper;

    private final String baseUrl = "http://localhost:8080";
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        public RestTemplate restTemplate(RestTemplateBuilder builder) {
            return builder.build();
        }
    }

    @BeforeEach
    void setUp() {
        // To inject properties in RestClientTest, we need to manually set them
        // or use a TestPropertySource. For simplicity, we'll reflectively set them.
        org.springframework.test.util.ReflectionTestUtils.setField(recapClient, "recapServerBaseUrl", baseUrl);
        org.springframework.test.util.ReflectionTestUtils.setField(recapClient, "recapServerApiKey", "test-api-key");
    }

    @Test
    @DisplayName("리캡 서버에 성공적으로 요청하고 응답을 받는다")
    void requestRecap_Success() throws Exception {
        // given
        InterviewScenario scenario = new InterviewScenario("Test Title", List.of()); // Simplified for test
        RecapServerRequest request = new RecapServerRequest("s3://audio-url", scenario);

        UUID sceneId = UUID.randomUUID();
        RecapContent recapContent = new RecapContent(sceneId, "This is an answer summary.");
        RecapServerResponse expectedResponse = new RecapServerResponse(List.of(recapContent));

        String expectedRequestJson = objectMapper.writeValueAsString(request);
        String expectedResponseJson = objectMapper.writeValueAsString(expectedResponse);

        mockServer.expect(requestTo(baseUrl + "/api/v1/recap"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedRequestJson))
                .andRespond(withSuccess(expectedResponseJson, MediaType.APPLICATION_JSON));

        // when
        Optional<RecapServerResponse> responseOptional = recapClient.requestRecap(request);

        // then
        mockServer.verify();
        assertThat(responseOptional).isPresent();
        RecapServerResponse actualResponse = responseOptional.get();
        assertThat(actualResponse.getRecapContent()).hasSize(1);
        assertThat(actualResponse.getRecapContent().get(0).getSceneId()).isEqualTo(sceneId);
        assertThat(actualResponse.getRecapContent().get(0).getAnswerSummary()).isEqualTo("This is an answer summary.");
    }
}
