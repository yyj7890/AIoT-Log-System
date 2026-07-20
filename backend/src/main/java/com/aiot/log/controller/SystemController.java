package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.service.impl.SystemRuntimeService;
import com.aiot.log.vo.SystemRuntimeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemRuntimeService systemRuntimeService;

    public SystemController(SystemRuntimeService systemRuntimeService) {
        this.systemRuntimeService = systemRuntimeService;
    }

    @GetMapping("/runtime")
    public ApiResponse<SystemRuntimeVO> getRuntime() {
        return ApiResponse.success(systemRuntimeService.getRuntime());
    }
}
