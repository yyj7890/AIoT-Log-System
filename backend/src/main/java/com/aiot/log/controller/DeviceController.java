package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.common.PageResult;
import com.aiot.log.dto.DeviceCreateRequest;
import com.aiot.log.dto.DeviceUpdateRequest;
import com.aiot.log.service.DeviceService;
import com.aiot.log.vo.DeviceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/devices")
@Tag(name = "设备管理", description = "通用 AIoT 设备的查询与维护")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    @Operation(summary = "分页查询设备", description = "可按关键字、设备类型和状态筛选")
    public ApiResponse<PageResult<DeviceVO>> listDevices(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(deviceService.listDevices(page, pageSize, keyword, type, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取设备详情")
    public ApiResponse<DeviceVO> getDevice(@PathVariable Long id) {
        return ApiResponse.success(deviceService.getDevice(id));
    }

    @PostMapping
    @Operation(summary = "创建设备")
    public ApiResponse<DeviceVO> createDevice(@Valid @RequestBody DeviceCreateRequest request) {
        return ApiResponse.success(deviceService.createDevice(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新设备")
    public ApiResponse<DeviceVO> updateDevice(@PathVariable Long id, @Valid @RequestBody DeviceUpdateRequest request) {
        return ApiResponse.success(deviceService.updateDevice(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除设备")
    public ApiResponse<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ApiResponse.success();
    }
}
