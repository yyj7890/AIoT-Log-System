package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.dto.AlertRuleRequest;
import com.aiot.log.service.AlertRuleService;
import com.aiot.log.vo.AlertRuleVO;
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
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    public AlertRuleController(AlertRuleService alertRuleService) {
        this.alertRuleService = alertRuleService;
    }

    @GetMapping
    public ApiResponse<List<AlertRuleVO>> listRules(
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(alertRuleService.listRules(deviceId, enabled));
    }

    @PostMapping
    public ApiResponse<AlertRuleVO> createRule(@Valid @RequestBody AlertRuleRequest request) {
        return ApiResponse.success(alertRuleService.createRule(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AlertRuleVO> updateRule(@PathVariable Long id, @Valid @RequestBody AlertRuleRequest request) {
        return ApiResponse.success(alertRuleService.updateRule(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        alertRuleService.deleteRule(id);
        return ApiResponse.success();
    }
}
