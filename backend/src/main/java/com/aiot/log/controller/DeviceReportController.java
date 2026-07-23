package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.common.PageResult;
import com.aiot.log.dto.DeviceReportCreateRequest;
import com.aiot.log.service.DeviceReportService;
import com.aiot.log.vo.DeviceReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "设备遥测", description = "设备 HTTP/MQTT 遥测数据查询和上报")
public class DeviceReportController {

    private final DeviceReportService deviceReportService;

    public DeviceReportController(DeviceReportService deviceReportService) {
        this.deviceReportService = deviceReportService;
    }

    @GetMapping
    @Operation(summary = "分页查询设备遥测", description = "可按设备和处理状态筛选")
    public ApiResponse<PageResult<DeviceReportVO>> listReports(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long pageSize,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(deviceReportService.listReports(page, pageSize, deviceId, status));
    }

    @PostMapping
    @Operation(summary = "上报设备遥测", description = "供设备或协议适配器通过 HTTP 写入遥测数据")
    public ApiResponse<DeviceReportVO> createReport(@Valid @RequestBody DeviceReportCreateRequest request) {
        return ApiResponse.success(deviceReportService.createReport(request));
    }
}
