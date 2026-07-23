package com.aiot.log.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:openapi;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=false",
                "mqtt.enabled=false",
                "mqtt.discovery.enabled=false",
                "springdoc.api-docs.enabled=true",
                "springdoc.swagger-ui.enabled=true"
        }
)
class OpenApiEndpointTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldExposeGroupedOpenApiDocument() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://127.0.0.1:" + port + "/v3/api-docs/aiot-api",
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"title\":\"AIoT 设备运行日志管理 API\""));
        assertTrue(response.getBody().contains("\"/api/system/runtime\""));
        assertTrue(response.getBody().contains("\"/api/logs\""));
        assertTrue(response.getBody().contains("\"/api/mqtt/status\""));
    }

    @Test
    void shouldExposeSwaggerUi() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://127.0.0.1:" + port + "/swagger-ui.html",
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("swagger-ui"));
    }
}
