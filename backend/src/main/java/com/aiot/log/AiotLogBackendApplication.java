package com.aiot.log;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.aiot.log.mapper")
@SpringBootApplication
public class AiotLogBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiotLogBackendApplication.class, args);
    }
}

