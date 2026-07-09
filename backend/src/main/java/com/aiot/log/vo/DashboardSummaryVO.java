package com.aiot.log.vo;

import java.util.List;

public class DashboardSummaryVO {

    private Long deviceTotal;
    private Long normalDeviceCount;
    private Long abnormalDeviceCount;
    private Long offlineDeviceCount;
    private Long maintenanceDeviceCount;
    private Long pendingLogCount;
    private List<LogBriefVO> recentErrorLogs;
    private List<LogBriefVO> recentMaintenanceLogs;

    public Long getDeviceTotal() {
        return deviceTotal;
    }

    public void setDeviceTotal(Long deviceTotal) {
        this.deviceTotal = deviceTotal;
    }

    public Long getNormalDeviceCount() {
        return normalDeviceCount;
    }

    public void setNormalDeviceCount(Long normalDeviceCount) {
        this.normalDeviceCount = normalDeviceCount;
    }

    public Long getAbnormalDeviceCount() {
        return abnormalDeviceCount;
    }

    public void setAbnormalDeviceCount(Long abnormalDeviceCount) {
        this.abnormalDeviceCount = abnormalDeviceCount;
    }

    public Long getOfflineDeviceCount() {
        return offlineDeviceCount;
    }

    public void setOfflineDeviceCount(Long offlineDeviceCount) {
        this.offlineDeviceCount = offlineDeviceCount;
    }

    public Long getMaintenanceDeviceCount() {
        return maintenanceDeviceCount;
    }

    public void setMaintenanceDeviceCount(Long maintenanceDeviceCount) {
        this.maintenanceDeviceCount = maintenanceDeviceCount;
    }

    public Long getPendingLogCount() {
        return pendingLogCount;
    }

    public void setPendingLogCount(Long pendingLogCount) {
        this.pendingLogCount = pendingLogCount;
    }

    public List<LogBriefVO> getRecentErrorLogs() {
        return recentErrorLogs;
    }

    public void setRecentErrorLogs(List<LogBriefVO> recentErrorLogs) {
        this.recentErrorLogs = recentErrorLogs;
    }

    public List<LogBriefVO> getRecentMaintenanceLogs() {
        return recentMaintenanceLogs;
    }

    public void setRecentMaintenanceLogs(List<LogBriefVO> recentMaintenanceLogs) {
        this.recentMaintenanceLogs = recentMaintenanceLogs;
    }
}

