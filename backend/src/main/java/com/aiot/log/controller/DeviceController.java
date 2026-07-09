package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.common.PageResult;
import com.aiot.log.dto.DeviceCreateRequest;
import com.aiot.log.dto.DeviceUpdateRequest;
import com.aiot.log.service.DeviceService;
import com.aiot.log.vo.DeviceVO;
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
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ApiResponse<PageResult<DeviceVO>> listDevices(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(deviceService.listDevices(page, pageSize, keyword, type, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceVO> getDevice(@PathVariable Long id) {
        return ApiResponse.success(deviceService.getDevice(id));
    }

    @PostMapping
    public ApiResponse<DeviceVO> createDevice(@Valid @RequestBody DeviceCreateRequest request) {
        return ApiResponse.success(deviceService.createDevice(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DeviceVO> updateDevice(@PathVariable Long id, @Valid @RequestBody DeviceUpdateRequest request) {
        return ApiResponse.success(deviceService.updateDevice(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ApiResponse.success();
    }
}
