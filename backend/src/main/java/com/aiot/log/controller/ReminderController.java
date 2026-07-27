package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.dto.ReminderRequest;
import com.aiot.log.service.ReminderService;
import com.aiot.log.vo.ReminderVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/reminders") @Tag(name = "提醒", description = "提醒持久化与到期触发；当前仅发布固定测试语音")
public class ReminderController {
    private final ReminderService service;
    public ReminderController(ReminderService service) { this.service = service; }
    @GetMapping public ApiResponse<List<ReminderVO>> list(@RequestParam(required = false) String deviceCode) { return ApiResponse.success(service.list(deviceCode)); }
    @PostMapping public ApiResponse<ReminderVO> create(@Valid @RequestBody ReminderRequest request) { return ApiResponse.success(service.create(request)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> cancel(@PathVariable Long id) { service.cancel(id); return ApiResponse.success(); }
}
