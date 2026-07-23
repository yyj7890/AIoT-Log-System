package com.aiot.log.service.impl;

import com.aiot.log.common.PageResult;
import com.aiot.log.dto.DeviceCreateRequest;
import com.aiot.log.dto.DeviceUpdateRequest;
import com.aiot.log.entity.Device;
import com.aiot.log.entity.DeviceReport;
import com.aiot.log.entity.LogRecord;
import com.aiot.log.enums.DeviceStatus;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.exception.ErrorCode;
import com.aiot.log.mapper.DeviceMapper;
import com.aiot.log.mapper.DeviceReportMapper;
import com.aiot.log.mapper.LogRecordMapper;
import com.aiot.log.service.DeviceService;
import com.aiot.log.vo.DeviceReportVO;
import com.aiot.log.vo.DeviceVO;
import com.aiot.log.vo.LogBriefVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceServiceImpl implements DeviceService {

    private static final BigDecimal TEMPERATURE_WARNING = new BigDecimal("80.00");
    private static final BigDecimal VOLTAGE_LOW_WARNING = new BigDecimal("210.00");
    private static final int SIGNAL_LOW_WARNING = -95;

    private final DeviceMapper deviceMapper;
    private final LogRecordMapper logRecordMapper;
    private final DeviceReportMapper deviceReportMapper;

    public DeviceServiceImpl(
            DeviceMapper deviceMapper,
            LogRecordMapper logRecordMapper,
            DeviceReportMapper deviceReportMapper) {
        this.deviceMapper = deviceMapper;
        this.logRecordMapper = logRecordMapper;
        this.deviceReportMapper = deviceReportMapper;
    }

    @Override
    public PageResult<DeviceVO> listDevices(Long page, Long pageSize, String keyword, String type, String status) {
        long current = page == null || page < 1 ? 1 : page;
        long size = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<Device>()
                .orderByDesc(Device::getCreatedAt);

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Device::getName, keyword)
                    .or()
                    .like(Device::getDeviceCode, keyword));
        }
        if (StringUtils.hasText(type)) {
            queryWrapper.eq(Device::getType, type);
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(Device::getStatus, status);
        }

        Page<Device> result = deviceMapper.selectPage(new Page<Device>(current, size), queryWrapper);
        List<DeviceVO> records = new ArrayList<DeviceVO>();
        for (Device device : result.getRecords()) {
            records.add(toVO(device));
        }
        return new PageResult<DeviceVO>(records, result.getTotal(), current, size);
    }

    @Override
    public DeviceVO getDevice(Long id) {
        Device device = getExistingDevice(id);
        DeviceVO vo = toVO(device);
        vo.setLogCount(countLogs(id, null, null));
        vo.setErrorLogCount(countLogs(id, "ERROR", null));
        vo.setPendingLogCount(countLogs(id, null, "PENDING"));
        vo.setRecentLogs(getRecentLogs(id));
        vo.setRecentReports(getRecentReports(id));
        return vo;
    }

    @Override
    public DeviceVO createDevice(DeviceCreateRequest request) {
        validateStatusOrDefault(request.getStatus());
        ensureDeviceCodeUnique(request.getDeviceCode());

        Device device = new Device();
        device.setName(request.getName());
        device.setDeviceCode(request.getDeviceCode());
        device.setType(request.getType());
        device.setLocation(request.getLocation());
        device.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : DeviceStatus.NORMAL);
        device.setDescription(request.getDescription());
        deviceMapper.insert(device);

        return toVO(deviceMapper.selectById(device.getId()));
    }

    @Override
    public DeviceVO updateDevice(Long id, DeviceUpdateRequest request) {
        validateStatusOrDefault(request.getStatus());
        Device device = getExistingDevice(id);
        device.setName(request.getName());
        device.setType(request.getType());
        device.setLocation(request.getLocation());
        device.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : DeviceStatus.NORMAL);
        device.setDescription(request.getDescription());
        deviceMapper.updateById(device);
        return toVO(deviceMapper.selectById(id));
    }

    @Override
    public void deleteDevice(Long id) {
        getExistingDevice(id);
        Long logCount = countLogs(id, null, null);
        if (logCount > 0) {
            throw new BusinessException(ErrorCode.DEVICE_HAS_LOGS);
        }
        deviceMapper.deleteById(id);
    }

    private Device getExistingDevice(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }
        return device;
    }

    private void ensureDeviceCodeUnique(String deviceCode) {
        Long count = deviceMapper.selectCount(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode));
        if (count > 0) {
            throw new BusinessException(ErrorCode.DEVICE_CODE_DUPLICATED);
        }
    }

    private void validateStatusOrDefault(String status) {
        if (StringUtils.hasText(status) && !DeviceStatus.isValid(status)) {
            throw new BusinessException(ErrorCode.DEVICE_STATUS_INVALID);
        }
    }

    private Long countLogs(Long deviceId, String logType, String status) {
        LambdaQueryWrapper<LogRecord> wrapper = new LambdaQueryWrapper<LogRecord>()
                .eq(LogRecord::getDeviceId, deviceId);
        if (StringUtils.hasText(logType)) {
            wrapper.eq(LogRecord::getLogType, logType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(LogRecord::getStatus, status);
        }
        return logRecordMapper.selectCount(wrapper);
    }

    private List<LogBriefVO> getRecentLogs(Long deviceId) {
        Page<LogRecord> page = logRecordMapper.selectPage(new Page<LogRecord>(1, 5),
                new LambdaQueryWrapper<LogRecord>()
                        .eq(LogRecord::getDeviceId, deviceId)
                        .orderByDesc(LogRecord::getCreatedAt));

        List<LogBriefVO> result = new ArrayList<LogBriefVO>();
        for (LogRecord logRecord : page.getRecords()) {
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
            vo.setSource(logRecord.getSource());
            vo.setCreatedAt(logRecord.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    private List<DeviceReportVO> getRecentReports(Long deviceId) {
        Page<DeviceReport> page = deviceReportMapper.selectPage(new Page<DeviceReport>(1, 5),
                new LambdaQueryWrapper<DeviceReport>()
                        .eq(DeviceReport::getDeviceId, deviceId)
                        .orderByDesc(DeviceReport::getReportedAt));

        List<DeviceReportVO> result = new ArrayList<DeviceReportVO>();
        Device device = deviceMapper.selectById(deviceId);
        for (DeviceReport report : page.getRecords()) {
            DeviceReportVO vo = new DeviceReportVO();
            vo.setId(report.getId());
            vo.setDeviceId(report.getDeviceId());
            if (device != null) {
                vo.setDeviceName(device.getName());
                vo.setDeviceCode(device.getDeviceCode());
            }
            vo.setTemperature(report.getTemperature());
            vo.setHumidity(report.getHumidity());
            vo.setVoltage(report.getVoltage());
            vo.setSignalStrength(report.getSignalStrength());
            vo.setStatus(report.getStatus());
            vo.setMessage(report.getMessage());
            vo.setAbnormal(isAbnormalReport(report));
            vo.setReportedAt(report.getReportedAt());
            vo.setCreatedAt(report.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    private boolean isAbnormalReport(DeviceReport report) {
        if (DeviceStatus.ABNORMAL.equals(report.getStatus())) {
            return true;
        }
        if (report.getTemperature() != null && report.getTemperature().compareTo(TEMPERATURE_WARNING) > 0) {
            return true;
        }
        if (report.getVoltage() != null && report.getVoltage().compareTo(VOLTAGE_LOW_WARNING) < 0) {
            return true;
        }
        return report.getSignalStrength() != null && report.getSignalStrength() < SIGNAL_LOW_WARNING;
    }

    private DeviceVO toVO(Device device) {
        DeviceVO vo = new DeviceVO();
        vo.setId(device.getId());
        vo.setName(device.getName());
        vo.setDeviceCode(device.getDeviceCode());
        vo.setType(device.getType());
        vo.setLocation(device.getLocation());
        vo.setStatus(device.getStatus());
        vo.setDescription(device.getDescription());
        vo.setLastOnlineAt(device.getLastOnlineAt());
        vo.setCreatedAt(device.getCreatedAt());
        vo.setUpdatedAt(device.getUpdatedAt());
        return vo;
    }
}
