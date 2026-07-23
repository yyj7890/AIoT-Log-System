package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Hidden
public class RootController {

    @GetMapping("/")
    public ApiResponse<Map<String, String>> root() {
        Map<String, String> info = new LinkedHashMap<String, String>();
        info.put("service", "AIoT Log Backend");
        info.put("status", "running");
        info.put("frontend", "http://127.0.0.1:5173/");
        info.put("apiEnums", "http://127.0.0.1:8080/api/enums");
        info.put("apiDashboard", "http://127.0.0.1:8080/api/dashboard/summary");
        return ApiResponse.success(info);
    }
}
