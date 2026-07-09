package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.common.PageResult;
import com.aiot.log.dto.DeviceReportCreateRequest;
import com.aiot.log.service.DeviceReportService;
import com.aiot.log.vo.DeviceReportVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/device-reports")
public class DeviceReportController {

    private final DeviceReportService deviceReportService;

    public DeviceReportController(DeviceReportService deviceReportService) {
        this.deviceReportService = deviceReportService;
    }

    @GetMapping
    public ApiResponse<PageResult<DeviceReportVO>> listReports(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long pageSize,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(deviceReportService.listReports(page, pageSize, deviceId, status));
    }

    @PostMapping
    public ApiResponse<DeviceReportVO> createReport(@Valid @RequestBody DeviceReportCreateRequest request) {
        return ApiResponse.success(deviceReportService.createReport(request));
    }
}
