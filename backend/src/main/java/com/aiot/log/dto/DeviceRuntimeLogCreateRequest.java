package com.aiot.log.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class DeviceRuntimeLogCreateRequest {

    @NotBlank(message = "设备编号不能为空")
    private String deviceCode;

    @Size(max = 64, message = "事件类型不能超过 64 个字符")
    private String eventType;

    @Size(max = 100, message = "日志标题不能超过 100 个字符")
    private String title;

    @Size(max = 20, message = "日志级别不能超过 20 个字符")
    private String level;

    @Size(max = 20, message = "日志类型不能超过 20 个字符")
    private String logType;

    @NotBlank(message = "日志内容不能为空")
    @Size(max = 2000, message = "日志内容不能超过 2000 个字符")
    private String message;

    private LocalDateTime reportedAt;

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }
}
