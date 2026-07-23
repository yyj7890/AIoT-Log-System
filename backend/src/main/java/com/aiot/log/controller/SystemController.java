package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.service.impl.SystemRuntimeService;
import com.aiot.log.vo.SystemRuntimeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@Tag(name = "系统状态", description = "轻量系统运行状态接口")
public class SystemController {

    private final SystemRuntimeService systemRuntimeService;

    public SystemController(SystemRuntimeService systemRuntimeService) {
        this.systemRuntimeService = systemRuntimeService;
    }

    @GetMapping("/runtime")
    @Operation(summary = "获取后端运行时长", description = "前端使用该接口探测后端在线状态")
    public ApiResponse<SystemRuntimeVO> getRuntime() {
        return ApiResponse.success(systemRuntimeService.getRuntime());
    }
}
