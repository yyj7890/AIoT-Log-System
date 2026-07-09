package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.common.PageResult;
import com.aiot.log.dto.LogCreateRequest;
import com.aiot.log.dto.LogStatusUpdateRequest;
import com.aiot.log.dto.LogUpdateRequest;
import com.aiot.log.service.LogService;
import com.aiot.log.vo.LogVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Validated
@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public ApiResponse<PageResult<LogVO>> listLogs(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long pageSize,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String logType,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return ApiResponse.success(logService.listLogs(
                page, pageSize, deviceId, logType, level, status, source, tagId, keyword, startTime, endTime));
    }

    @GetMapping("/{id}")
    public ApiResponse<LogVO> getLog(@PathVariable Long id) {
        return ApiResponse.success(logService.getLog(id));
    }

    @PostMapping
    public ApiResponse<LogVO> createLog(@Valid @RequestBody LogCreateRequest request) {
        return ApiResponse.success(logService.createLog(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<LogVO> updateLog(@PathVariable Long id, @Valid @RequestBody LogUpdateRequest request) {
        return ApiResponse.success(logService.updateLog(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<LogVO> updateLogStatus(
            @PathVariable Long id,
            @Valid @RequestBody LogStatusUpdateRequest request) {
        return ApiResponse.success(logService.updateLogStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteLog(@PathVariable Long id) {
        logService.deleteLog(id);
        return ApiResponse.success();
    }
}
