package com.aiot.log.vo;

import java.time.LocalDateTime;
import java.util.List;

public class DeviceVO {

    private Long id;
    private String name;
    private String deviceCode;
    private String type;
    private String location;
    private String status;
    private String description;
    private LocalDateTime lastOnlineAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long logCount;
    private Long errorLogCount;
    private Long pendingLogCount;
    private List<LogBriefVO> recentLogs;
    private List<DeviceReportVO> recentReports;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getLastOnlineAt() {
        return lastOnlineAt;
    }

    public void setLastOnlineAt(LocalDateTime lastOnlineAt) {
        this.lastOnlineAt = lastOnlineAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getLogCount() {
        return logCount;
    }

    public void setLogCount(Long logCount) {
        this.logCount = logCount;
    }

    public Long getErrorLogCount() {
        return errorLogCount;
    }

    public void setErrorLogCount(Long errorLogCount) {
        this.errorLogCount = errorLogCount;
    }

    public Long getPendingLogCount() {
        return pendingLogCount;
    }

    public void setPendingLogCount(Long pendingLogCount) {
        this.pendingLogCount = pendingLogCount;
    }

    public List<LogBriefVO> getRecentLogs() {
        return recentLogs;
    }

    public void setRecentLogs(List<LogBriefVO> recentLogs) {
        this.recentLogs = recentLogs;
    }

    public List<DeviceReportVO> getRecentReports() {
        return recentReports;
    }

    public void setRecentReports(List<DeviceReportVO> recentReports) {
        this.recentReports = recentReports;
    }
}
