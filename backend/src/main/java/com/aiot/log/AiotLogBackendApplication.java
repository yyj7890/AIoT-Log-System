package com.aiot.log;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@MapperScan("com.aiot.log.mapper")
@SpringBootApplication
public class AiotLogBackendApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(AiotLogBackendApplication.class);
        application.setDefaultProperties(Map.of(
                "management.server.address", "127.0.0.1",
                "management.server.port", "8081",
                "management.endpoints.web.exposure.include", "health",
                "management.endpoint.health.show-details", "never",
                "management.endpoint.health.probes.enabled", "true"));
        application.run(args);
    }
}

