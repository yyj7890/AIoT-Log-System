package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.service.DashboardService;
import com.aiot.log.vo.DashboardSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "首页统计", description = "设备和日志汇总统计")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "获取首页汇总")
    public ApiResponse<DashboardSummaryVO> getSummary() {
        return ApiResponse.success(dashboardService.getSummary());
    }
}

