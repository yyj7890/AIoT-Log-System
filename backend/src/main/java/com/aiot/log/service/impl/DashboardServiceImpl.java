package com.aiot.log.service.impl;

import com.aiot.log.entity.Device;
import com.aiot.log.entity.LogRecord;
import com.aiot.log.enums.DeviceStatus;
import com.aiot.log.enums.LogStatus;
import com.aiot.log.enums.LogType;
import com.aiot.log.mapper.DeviceMapper;
import com.aiot.log.mapper.LogRecordMapper;
import com.aiot.log.service.DashboardService;
import com.aiot.log.vo.DashboardSummaryVO;
import com.aiot.log.vo.LogBriefVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final DeviceMapper deviceMapper;
    private final LogRecordMapper logRecordMapper;

    public DashboardServiceImpl(DeviceMapper deviceMapper, LogRecordMapper logRecordMapper) {
        this.deviceMapper = deviceMapper;
        this.logRecordMapper = logRecordMapper;
    }

    @Override
    public DashboardSummaryVO getSummary() {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setDeviceTotal(deviceMapper.selectCount(null));
        vo.setNormalDeviceCount(countDevicesByStatus(DeviceStatus.NORMAL));
        vo.setAbnormalDeviceCount(countDevicesByStatus(DeviceStatus.ABNORMAL));
        vo.setOfflineDeviceCount(countDevicesByStatus(DeviceStatus.OFFLINE));
        vo.setMaintenanceDeviceCount(countDevicesByStatus(DeviceStatus.MAINTENANCE));
        vo.setPendingLogCount(logRecordMapper.selectCount(new LambdaQueryWrapper<LogRecord>()
                .eq(LogRecord::getStatus, LogStatus.PENDING)));
        vo.setRecentErrorLogs(findRecentLogs(LogType.ERROR, 5));
        vo.setRecentMaintenanceLogs(findRecentLogs(LogType.MAINTENANCE, 5));
        return vo;
    }

    private Long countDevicesByStatus(String status) {
        return deviceMapper.selectCount(new LambdaQueryWrapper<Device>().eq(Device::getStatus, status));
    }

    private List<LogBriefVO> findRecentLogs(String logType, long size) {
        Page<LogRecord> page = logRecordMapper.selectPage(new Page<LogRecord>(1, size),
                new LambdaQueryWrapper<LogRecord>()
                        .eq(LogRecord::getLogType, logType)
                        .orderByDesc(LogRecord::getCreatedAt));
        List<LogBriefVO> result = new ArrayList<LogBriefVO>();
        for (LogRecord logRecord : page.getRecords()) {
            result.add(toBriefVO(logRecord));
        }
        return result;
    }

    private LogBriefVO toBriefVO(LogRecord logRecord) {
        Device device = deviceMapper.selectById(logRecord.getDeviceId());
        LogBriefVO vo = new LogBriefVO();
        vo.setId(logRecord.getId());
        vo.setDeviceId(logRecord.getDeviceId());
        if (device != null) {
            vo.setDeviceName(device.getName());
            vo.setDeviceCode(device.getDeviceCode());
        }
        vo.setTitle(logRecord.getTitle());
        vo.setLogType(logRecord.getLogType());
        vo.setLevel(logRecord.getLevel());
        vo.setStatus(logRecord.getStatus());
        vo.setCreatedAt(logRecord.getCreatedAt());
        return vo;
    }
}

