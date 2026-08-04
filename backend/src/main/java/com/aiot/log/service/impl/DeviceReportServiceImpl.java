package com.aiot.log.service.impl;

import com.aiot.log.common.PageResult;
import com.aiot.log.dto.DeviceReportCreateRequest;
import com.aiot.log.entity.AlertRule;
import com.aiot.log.entity.Device;
import com.aiot.log.entity.DeviceReport;
import com.aiot.log.entity.LogRecord;
import com.aiot.log.enums.DeviceStatus;
import com.aiot.log.enums.LogLevel;
import com.aiot.log.enums.LogSource;
import com.aiot.log.enums.LogStatus;
import com.aiot.log.enums.LogType;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.exception.ErrorCode;
import com.aiot.log.mapper.AlertRuleMapper;
import com.aiot.log.mapper.DeviceMapper;
import com.aiot.log.mapper.DeviceReportMapper;
import com.aiot.log.mapper.LogRecordMapper;
import com.aiot.log.service.DeviceReportService;
import com.aiot.log.service.EnvironmentAnnouncementRuleEvaluator;
import com.aiot.log.vo.DeviceReportVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceReportServiceImpl implements DeviceReportService {

    private static final BigDecimal TEMPERATURE_WARNING = new BigDecimal("80.00");
    private static final BigDecimal VOLTAGE_LOW_WARNING = new BigDecimal("210.00");
    private static final int SIGNAL_LOW_WARNING = -95;

    private final DeviceReportMapper deviceReportMapper;
    private final DeviceMapper deviceMapper;
    private final LogRecordMapper logRecordMapper;
    private final AlertRuleMapper alertRuleMapper;
    private final EnvironmentAnnouncementRuleEvaluator environmentAnnouncementRuleEvaluator;

    public DeviceReportServiceImpl(
            DeviceReportMapper deviceReportMapper,
            DeviceMapper deviceMapper,
            LogRecordMapper logRecordMapper,
            AlertRuleMapper alertRuleMapper, @Lazy EnvironmentAnnouncementRuleEvaluator environmentAnnouncementRuleEvaluator) {
        this.deviceReportMapper = deviceReportMapper;
        this.deviceMapper = deviceMapper;
        this.logRecordMapper = logRecordMapper;
        this.alertRuleMapper = alertRuleMapper;
        this.environmentAnnouncementRuleEvaluator = environmentAnnouncementRuleEvaluator;
    }

    @Override
    public DeviceReportVO createReport(DeviceReportCreateRequest request) {
        Device device = getDeviceByCode(request.getDeviceCode());
        String reportStatus = StringUtils.hasText(request.getStatus()) ? request.getStatus() : DeviceStatus.NORMAL;
        validateStatus(reportStatus);

        DeviceReport report = new DeviceReport();
        report.setDeviceId(device.getId());
        report.setTemperature(request.getTemperature());
        report.setHumidity(request.getHumidity());
        report.setPressure(request.getPressure());
        report.setIlluminance(request.getIlluminance());
        report.setVoltage(request.getVoltage());
        report.setBatteryPercent(request.getBatteryPercent());
        report.setCharging(request.getCharging());
        report.setSignalStrength(request.getSignalStrength());
        report.setStatus(reportStatus);
        report.setMessage(request.getMessage());
        report.setReportedAt(request.getReportedAt() == null ? LocalDateTime.now() : request.getReportedAt());
        deviceReportMapper.insert(report);
        environmentAnnouncementRuleEvaluator.evaluateIndoor(report);

        List<AlertRule> triggeredRules = findTriggeredRules(report);
        boolean abnormal = DeviceStatus.ABNORMAL.equals(report.getStatus()) || !triggeredRules.isEmpty();
        Long generatedLogId = null;
        if (abnormal) {
            generatedLogId = createAbnormalLog(device, report, triggeredRules);
            device.setStatus(DeviceStatus.ABNORMAL);
        } else if (!DeviceStatus.MAINTENANCE.equals(device.getStatus())) {
            device.setStatus(reportStatus);
        }
        device.setLastOnlineAt(report.getReportedAt());
        deviceMapper.updateById(device);

        DeviceReportVO vo = toVO(report, device);
        vo.setAbnormal(abnormal);
        vo.setGeneratedLogId(generatedLogId);
        return vo;
    }

    @Override
    public PageResult<DeviceReportVO> listReports(Long page, Long pageSize, Long deviceId, String status) {
        long current = page == null || page < 1 ? 1 : page;
        long size = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<DeviceReport> queryWrapper = new LambdaQueryWrapper<DeviceReport>()
                .orderByDesc(DeviceReport::getReportedAt);
        if (deviceId != null) {
            queryWrapper.eq(DeviceReport::getDeviceId, deviceId);
        }
        if (StringUtils.hasText(status)) {
            validateStatus(status);
            queryWrapper.eq(DeviceReport::getStatus, status);
        }

        Page<DeviceReport> result = deviceReportMapper.selectPage(new Page<DeviceReport>(current, size), queryWrapper);
        List<DeviceReportVO> records = new ArrayList<DeviceReportVO>();
        for (DeviceReport report : result.getRecords()) {
            Device device = deviceMapper.selectById(report.getDeviceId());
            records.add(toVO(report, device));
        }
        return new PageResult<DeviceReportVO>(records, result.getTotal(), current, size);
    }

    private Device getDeviceByCode(String deviceCode) {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode));
        if (device == null) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }
        return device;
    }

    private void validateStatus(String status) {
        if (!DeviceStatus.isValid(status)) {
            throw new BusinessException(ErrorCode.DEVICE_STATUS_INVALID);
        }
    }

    private boolean isAbnormal(DeviceReport report) {
        return DeviceStatus.ABNORMAL.equals(report.getStatus()) || !findTriggeredRules(report).isEmpty();
    }

    private List<AlertRule> findTriggeredRules(DeviceReport report) {
        List<AlertRule> rules = alertRuleMapper.selectList(new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getEnabled, true)
                .and(wrapper -> wrapper
                        .isNull(AlertRule::getDeviceId)
                        .or()
                        .eq(AlertRule::getDeviceId, report.getDeviceId())));

        List<AlertRule> triggered = new ArrayList<AlertRule>();
        if (rules.isEmpty()) {
            if (isDefaultThresholdAbnormal(report)) {
                AlertRule fallbackRule = new AlertRule();
                fallbackRule.setName("默认异常阈值");
                fallbackRule.setLevel(LogLevel.ERROR);
                triggered.add(fallbackRule);
            }
            return triggered;
        }

        for (AlertRule rule : rules) {
            if (isRuleTriggered(rule, report)) {
                triggered.add(rule);
            }
        }
        return triggered;
    }

    private boolean isDefaultThresholdAbnormal(DeviceReport report) {
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

    private boolean isRuleTriggered(AlertRule rule, DeviceReport report) {
        BigDecimal value = metricValue(rule.getMetric(), report);
        if (value == null) {
            return false;
        }
        int compare = value.compareTo(rule.getThresholdValue());
        if ("GT".equals(rule.getOperator())) {
            return compare > 0;
        }
        if ("LT".equals(rule.getOperator())) {
            return compare < 0;
        }
        if ("GTE".equals(rule.getOperator())) {
            return compare >= 0;
        }
        if ("LTE".equals(rule.getOperator())) {
            return compare <= 0;
        }
        if ("EQ".equals(rule.getOperator())) {
            return compare == 0;
        }
        return false;
    }

    private BigDecimal metricValue(String metric, DeviceReport report) {
        if ("temperature".equals(metric)) {
            return report.getTemperature();
        }
        if ("humidity".equals(metric)) {
            return report.getHumidity();
        }
        if ("pressure".equals(metric)) {
            return report.getPressure();
        }
        if ("illuminance".equals(metric)) {
            return report.getIlluminance();
        }
        if ("voltage".equals(metric)) {
            return report.getVoltage();
        }
        if ("signalStrength".equals(metric) && report.getSignalStrength() != null) {
            return new BigDecimal(report.getSignalStrength());
        }
        return null;
    }

    private Long createAbnormalLog(Device device, DeviceReport report, List<AlertRule> triggeredRules) {
        LogRecord logRecord = new LogRecord();
        logRecord.setDeviceId(device.getId());
        logRecord.setTitle("设备上报异常：" + device.getName());
        logRecord.setContent(buildAbnormalContent(report, triggeredRules));
        logRecord.setLogType(LogType.ERROR);
        logRecord.setLevel(resolveLogLevel(triggeredRules));
        logRecord.setStatus(LogStatus.PENDING);
        logRecord.setSource(LogSource.DEVICE);
        logRecordMapper.insert(logRecord);
        return logRecord.getId();
    }

    private String resolveLogLevel(List<AlertRule> triggeredRules) {
        for (AlertRule rule : triggeredRules) {
            if (LogLevel.ERROR.equals(rule.getLevel())) {
                return LogLevel.ERROR;
            }
        }
        return LogLevel.WARNING;
    }

    private String buildAbnormalContent(DeviceReport report, List<AlertRule> triggeredRules) {
        StringBuilder builder = new StringBuilder();
        builder.append("设备自动上报触发异常记录。");
        if (report.getTemperature() != null) {
            builder.append(" 温度=").append(report.getTemperature()).append("℃。");
        }
        if (report.getHumidity() != null) {
            builder.append(" 湿度=").append(report.getHumidity()).append("%。");
        }
        if (report.getPressure() != null) {
            builder.append(" 气压=").append(report.getPressure()).append("hPa。");
        }
        if (report.getIlluminance() != null) {
            builder.append(" 光照=").append(report.getIlluminance()).append("lux。");
        }
        if (report.getVoltage() != null) {
            builder.append(" 电压=").append(report.getVoltage()).append("V。");
        }
        if (report.getSignalStrength() != null) {
            builder.append(" 信号=").append(report.getSignalStrength()).append("dBm。");
        }
        if (StringUtils.hasText(report.getMessage())) {
            builder.append(" 说明=").append(report.getMessage());
        }
        if (!triggeredRules.isEmpty()) {
            builder.append(" 命中规则=");
            for (int i = 0; i < triggeredRules.size(); i++) {
                if (i > 0) {
                    builder.append("、");
                }
                builder.append(triggeredRules.get(i).getName());
            }
            builder.append("。");
        }
        return builder.toString();
    }

    private DeviceReportVO toVO(DeviceReport report, Device device) {
        DeviceReportVO vo = new DeviceReportVO();
        vo.setId(report.getId());
        vo.setDeviceId(report.getDeviceId());
        if (device != null) {
            vo.setDeviceName(device.getName());
            vo.setDeviceCode(device.getDeviceCode());
        }
        vo.setTemperature(report.getTemperature());
        vo.setHumidity(report.getHumidity());
        vo.setPressure(report.getPressure());
        vo.setIlluminance(report.getIlluminance());
        vo.setVoltage(report.getVoltage());
        vo.setBatteryPercent(report.getBatteryPercent());
        vo.setCharging(report.getCharging());
        vo.setSignalStrength(report.getSignalStrength());
        vo.setStatus(report.getStatus());
        vo.setMessage(report.getMessage());
        vo.setAbnormal(isAbnormal(report));
        vo.setReportedAt(report.getReportedAt());
        vo.setCreatedAt(report.getCreatedAt());
        return vo;
    }
}
