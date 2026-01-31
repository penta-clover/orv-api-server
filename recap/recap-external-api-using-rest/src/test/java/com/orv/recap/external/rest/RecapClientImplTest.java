package com.orv.recap.external.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orv.recap.external.dto.RecapServerRequest;
import com.orv.recap.external.dto.RecapServerResponse;
import com.orv.recap.domain.InterviewScenario;
import com.orv.recap.domain.RecapContent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecapClientImplTest {

    @InjectMocks
    private RecapClientImpl recapClient;

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final String baseUrl = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recapClient, "recapServerBaseUrl", baseUrl);
        ReflectionTestUtils.setField(recapClient, "recapServerApiKey", "test-api-key");
    }

    @Test
    @DisplayName("리캡 서버에 성공적으로 요청하고 응답을 받는다")
    void requestRecap_Success() throws Exception {
        // given
        InterviewScenario scenario = new InterviewScenario("Test Title", List.of());
        RecapServerRequest request = new RecapServerRequest("s3://audio-url", scenario);

        UUID sceneId = UUID.randomUUID();
        RecapContent recapContent = new RecapContent(sceneId, "This is an answer summary.");
        RecapServerResponse expectedResponse = new RecapServerResponse(List.of(recapContent));

        when(restTemplate.exchange(
                eq(baseUrl + "/api/v1/recap"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(RecapServerResponse.class)
        )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // when
        Optional<RecapServerResponse> responseOptional = recapClient.requestRecap(request);

        // then
        assertThat(responseOptional).isPresent();
        RecapServerResponse actualResponse = responseOptional.get();
        assertThat(actualResponse.getRecapContent()).hasSize(1);
        assertThat(actualResponse.getRecapContent().get(0).getSceneId()).isEqualTo(sceneId);
        assertThat(actualResponse.getRecapContent().get(0).getAnswerSummary()).isEqualTo("This is an answer summary.");

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq(baseUrl + "/api/v1/recap"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(RecapServerResponse.class)
        );

        HttpEntity<?> capturedEntity = entityCaptor.getValue();
        assertThat(capturedEntity.getHeaders().getFirst("Authorization")).isEqualTo("Bearer test-api-key");
        assertThat(capturedEntity.getHeaders().getFirst("Content-Type")).isEqualTo("application/json");
        assertThat(capturedEntity.getBody()).isEqualTo(request);
    }
}
