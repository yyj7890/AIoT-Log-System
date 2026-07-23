package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.dto.AlertRuleRequest;
import com.aiot.log.service.AlertRuleService;
import com.aiot.log.vo.AlertRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/alert-rules")
@Tag(name = "告警规则", description = "设备遥测阈值告警规则管理")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    public AlertRuleController(AlertRuleService alertRuleService) {
        this.alertRuleService = alertRuleService;
    }

    @GetMapping
    @Operation(summary = "查询告警规则", description = "可按设备和启用状态筛选告警规则")
    public ApiResponse<List<AlertRuleVO>> listRules(
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(alertRuleService.listRules(deviceId, enabled));
    }

    @PostMapping
    @Operation(summary = "创建告警规则")
    public ApiResponse<AlertRuleVO> createRule(@Valid @RequestBody AlertRuleRequest request) {
        return ApiResponse.success(alertRuleService.createRule(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新告警规则")
    public ApiResponse<AlertRuleVO> updateRule(@PathVariable Long id, @Valid @RequestBody AlertRuleRequest request) {
        return ApiResponse.success(alertRuleService.updateRule(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除告警规则")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        alertRuleService.deleteRule(id);
        return ApiResponse.success();
    }
}
