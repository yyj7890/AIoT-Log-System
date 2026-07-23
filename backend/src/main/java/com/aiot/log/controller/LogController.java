package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.common.PageResult;
import com.aiot.log.dto.LogCreateRequest;
import com.aiot.log.dto.LogBatchDeleteRequest;
import com.aiot.log.dto.LogStatusUpdateRequest;
import com.aiot.log.dto.LogUpdateRequest;
import com.aiot.log.service.LogService;
import com.aiot.log.vo.LogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "日志管理", description = "运行、异常、维护和巡检日志的查询与维护")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    @Operation(summary = "分页查询日志", description = "支持设备、类型、级别、状态、来源、标签、关键字和时间范围筛选")
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
    @Operation(summary = "获取日志详情")
    public ApiResponse<LogVO> getLog(@PathVariable Long id) {
        return ApiResponse.success(logService.getLog(id));
    }

    @PostMapping
    @Operation(summary = "创建人工日志")
    public ApiResponse<LogVO> createLog(@Valid @RequestBody LogCreateRequest request) {
        return ApiResponse.success(logService.createLog(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新日志")
    public ApiResponse<LogVO> updateLog(@PathVariable Long id, @Valid @RequestBody LogUpdateRequest request) {
        return ApiResponse.success(logService.updateLog(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "更新日志处理状态")
    public ApiResponse<LogVO> updateLogStatus(
            @PathVariable Long id,
            @Valid @RequestBody LogStatusUpdateRequest request) {
        return ApiResponse.success(logService.updateLogStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除单条日志")
    public ApiResponse<Void> deleteLog(@PathVariable Long id) {
        logService.deleteLog(id);
        return ApiResponse.success();
    }

    @DeleteMapping
    @Operation(summary = "批量删除日志")
    public ApiResponse<Void> deleteLogs(@Valid @RequestBody LogBatchDeleteRequest request) {
        logService.deleteLogs(request.getIds());
        return ApiResponse.success();
    }
}
