package com.aiot.log.controller;

import com.aiot.log.announcement.FixedTestAnnouncementService;
import com.aiot.log.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
@Tag(name = "主动播报测试", description = "默认关闭的固定 Opus 测试播报接口")
public class AnnouncementController {
    private final FixedTestAnnouncementService fixedTestAnnouncementService;

    public AnnouncementController(FixedTestAnnouncementService fixedTestAnnouncementService) {
        this.fixedTestAnnouncementService = fixedTestAnnouncementService;
    }

    @PostMapping("/fixed-test")
    @Operation(summary = "发布固定 Opus 测试播报", description = "仅在 announcement.test-enabled=true 时可用；不会生成或部署 TTS")
    public ApiResponse<Map<String, String>> publishFixedTest(@RequestParam String deviceCode) {
        String taskId = fixedTestAnnouncementService.publish(deviceCode);
        return ApiResponse.success(Map.of("taskId", taskId, "status", "PUBLISHED"));
    }
}
