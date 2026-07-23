package com.aiot.log.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:error-response;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=false",
                "mqtt.enabled=false",
                "mqtt.discovery.enabled=false"
        }
)
class GlobalExceptionHandlerEndpointTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnStableBusinessErrorCodeAndHttpStatus() throws Exception {
        HttpHeaders headers = jsonHeaders();
        headers.set("X-Trace-Id", "client-trace-1234");
        String body = """
                {
                  "name": "测试设备",
                  "deviceCode": "ERROR-CODE-TEST",
                  "type": "TEST",
                  "status": "INVALID"
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/devices"),
                HttpMethod.POST,
                new HttpEntity<String>(body, headers),
                String.class
        );

        assertError(response, HttpStatus.BAD_REQUEST, "DEVICE_STATUS_INVALID");
        assertEquals("client-trace-1234", response.getHeaders().getFirst("X-Trace-Id"));
        assertEquals(
                "client-trace-1234",
                objectMapper.readTree(response.getBody()).path("traceId").asText()
        );
    }

    @Test
    void shouldReturnValidationErrorWithoutExposingRequestBody() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/devices"),
                HttpMethod.POST,
                new HttpEntity<String>(
                        """
                                {
                                  "deviceCode": "VALIDATION-TEST",
                                  "type": "TEST"
                                }
                                """,
                        jsonHeaders()),
                String.class
        );

        assertError(response, HttpStatus.BAD_REQUEST, "REQUEST_VALIDATION_FAILED");
        assertEquals("设备名称不能为空", objectMapper.readTree(response.getBody()).path("message").asText());
    }

    @Test
    void shouldReturnParameterAndBodyFormatErrors() throws Exception {
        ResponseEntity<String> parameterResponse = restTemplate.getForEntity(
                url("/api/devices/not-a-number"),
                String.class
        );
        assertError(parameterResponse, HttpStatus.BAD_REQUEST, "REQUEST_PARAMETER_INVALID");

        ResponseEntity<String> bodyResponse = restTemplate.exchange(
                url("/api/devices"),
                HttpMethod.POST,
                new HttpEntity<String>("{invalid-json", jsonHeaders()),
                String.class
        );
        assertError(bodyResponse, HttpStatus.BAD_REQUEST, "REQUEST_BODY_INVALID");
    }

    @Test
    void shouldReturnUnifiedNotFoundResponse() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/api/not-existing"),
                String.class
        );

        assertError(response, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    private void assertError(
            ResponseEntity<String> response,
            HttpStatus expectedStatus,
            String expectedErrorCode) throws Exception {
        assertEquals(expectedStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(expectedStatus.value(), body.path("code").asInt());
        assertEquals(expectedErrorCode, body.path("errorCode").asText());
        assertFalse(body.path("traceId").asText().isBlank());
        assertEquals(body.path("traceId").asText(), response.getHeaders().getFirst("X-Trace-Id"));
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }
}
