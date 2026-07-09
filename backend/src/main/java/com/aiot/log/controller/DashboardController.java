package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.service.DashboardService;
import com.aiot.log.vo.DashboardSummaryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryVO> getSummary() {
        return ApiResponse.success(dashboardService.getSummary());
    }
}

