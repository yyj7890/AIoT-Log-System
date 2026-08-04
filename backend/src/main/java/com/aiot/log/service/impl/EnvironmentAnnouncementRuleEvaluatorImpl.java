package com.aiot.log.service.impl;

import com.aiot.log.announcement.TextAnnouncementService;
import com.aiot.log.config.AnnouncementProperties;
import com.aiot.log.entity.Device;
import com.aiot.log.entity.DeviceReport;
import com.aiot.log.entity.EnvironmentAnnouncementEvent;
import com.aiot.log.entity.EnvironmentAnnouncementRule;
import com.aiot.log.entity.EnvironmentOutdoorReading;
import com.aiot.log.entity.EnvironmentSpace;
import com.aiot.log.mapper.DeviceMapper;
import com.aiot.log.mapper.EnvironmentAnnouncementEventMapper;
import com.aiot.log.mapper.EnvironmentAnnouncementRuleMapper;
import com.aiot.log.mapper.EnvironmentOutdoorReadingMapper;
import com.aiot.log.mapper.EnvironmentSpaceMapper;
import com.aiot.log.service.EnvironmentAnnouncementRuleEvaluator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class EnvironmentAnnouncementRuleEvaluatorImpl implements EnvironmentAnnouncementRuleEvaluator {
    private final EnvironmentAnnouncementRuleMapper rules;
    private final EnvironmentAnnouncementEventMapper events;
    private final EnvironmentOutdoorReadingMapper readings;
    private final EnvironmentSpaceMapper spaces;
    private final DeviceMapper devices;
    private final TextAnnouncementService announcements;
    private final AnnouncementProperties announcementProperties;

    public EnvironmentAnnouncementRuleEvaluatorImpl(EnvironmentAnnouncementRuleMapper rules, EnvironmentAnnouncementEventMapper events,
            EnvironmentOutdoorReadingMapper readings, EnvironmentSpaceMapper spaces, DeviceMapper devices,
            TextAnnouncementService announcements, AnnouncementProperties announcementProperties) {
        this.rules = rules; this.events = events; this.readings = readings; this.spaces = spaces; this.devices = devices;
        this.announcements = announcements; this.announcementProperties = announcementProperties;
    }

    @Override public void evaluate(EnvironmentOutdoorReading reading) {
        EnvironmentSpace space = spaces.selectById(reading.getSpaceId());
        if (space == null || !Boolean.TRUE.equals(space.getEnabled())) return;
        for (EnvironmentAnnouncementRule rule : enabledRules(space.getId())) {
            if (rule.metric.startsWith("indoor_")) continue;
            if ("weather_change".equals(rule.metric)) {
                EnvironmentOutdoorReading prior = previousReading(reading);
                if (prior == null || blank(prior.weatherText) || blank(reading.weatherText) || prior.weatherText.equals(reading.weatherText)) continue;
                process(rule, space, null, "室外天气由" + prior.weatherText + "转为" + reading.weatherText + "。", reading.getId());
                continue;
            }
            BigDecimal value = outdoorValue(reading, rule.metric);
            if (value != null && matchesOutdoor(rule, reading, value)) process(rule, space, value, outdoorMessage(rule.metric, value), reading.getId());
            else if (value != null) clear(rule, space, value, reading.getId());
        }
    }

    @Override public void evaluateIndoor(DeviceReport report) {
        for (EnvironmentSpace space : spaces.selectList(new LambdaQueryWrapper<EnvironmentSpace>()
                .eq(EnvironmentSpace::getIndoorSensorDeviceId, report.getDeviceId()).eq(EnvironmentSpace::getEnabled, true))) {
            for (EnvironmentAnnouncementRule rule : enabledRules(space.getId())) {
                if (!rule.metric.startsWith("indoor_")) continue;
                BigDecimal value = indoorValue(report, rule.metric);
                if (value != null && thresholdMatches(rule, value)) process(rule, space, value, indoorMessage(rule.metric, value), report.getId());
                else if (value != null) clear(rule, space, value, report.getId());
            }
        }
    }

    private List<EnvironmentAnnouncementRule> enabledRules(Long spaceId) {
        return rules.selectList(new LambdaQueryWrapper<EnvironmentAnnouncementRule>()
                .eq(EnvironmentAnnouncementRule::getSpaceId, spaceId).eq(EnvironmentAnnouncementRule::getEnabled, true));
    }

    private void process(EnvironmentAnnouncementRule rule, EnvironmentSpace space, BigDecimal value, String observation, Long sourceId) {
        EnvironmentAnnouncementEvent event = event(rule, space, value, observation, sourceId);
        if (!confirmed(rule)) { event.status = "OBSERVED"; events.insert(event); return; }
        if (inQuietPeriod(rule, LocalTime.now()) || inCooldown(rule) || !announcementProperties.getTts().isEnabled()
                || space.getPrimarySpeakerDeviceId() == null) { event.status = "SUPPRESSED"; events.insert(event); return; }
        Device speaker = devices.selectById(space.getPrimarySpeakerDeviceId());
        if (speaker == null) { event.status = "SUPPRESSED"; events.insert(event); return; }
        try { event.announcementTaskId = announcements.publish(speaker.getDeviceCode(), event.message); event.status = "PUBLISHED"; }
        catch (RuntimeException ignored) { event.status = "FAILED"; }
        events.insert(event);
    }

    private void clear(EnvironmentAnnouncementRule rule, EnvironmentSpace space, BigDecimal value, Long sourceId) {
        EnvironmentAnnouncementEvent event = event(rule, space, value, "当前读数已恢复至规则范围内。", sourceId);
        event.status = "CLEAR"; events.insert(event);
    }

    private EnvironmentAnnouncementEvent event(EnvironmentAnnouncementRule rule, EnvironmentSpace space, BigDecimal value, String observation, Long sourceId) {
        EnvironmentAnnouncementEvent event = new EnvironmentAnnouncementEvent();
        event.ruleId = rule.id; event.spaceId = space.getId(); event.eventKey = rule.id + ":" + sourceId;
        event.observedValue = value; event.message = rule.name + "，" + observation; event.createdAt = LocalDateTime.now(); return event;
    }

    private BigDecimal outdoorValue(EnvironmentOutdoorReading reading, String metric) {
        if ("temperature".equals(metric)) return reading.temperature;
        if ("aqi".equals(metric) && reading.aqi != null) return BigDecimal.valueOf(reading.aqi);
        if ("precipitation".equals(metric)) return reading.precip;
        return null;
    }

    private BigDecimal indoorValue(DeviceReport report, String metric) {
        if ("indoor_temperature".equals(metric)) return report.getTemperature();
        if ("indoor_humidity".equals(metric)) return report.getHumidity();
        if ("indoor_pressure".equals(metric)) return report.getPressure();
        if ("indoor_illuminance".equals(metric)) return report.getIlluminance();
        return null;
    }

    private boolean matchesOutdoor(EnvironmentAnnouncementRule rule, EnvironmentOutdoorReading reading, BigDecimal value) {
        if (thresholdMatches(rule, value)) return true;
        if (rule.changeValue == null) return false;
        EnvironmentOutdoorReading prior = previousReading(reading);
        BigDecimal before = prior == null ? null : outdoorValue(prior, rule.metric);
        return before != null && value.subtract(before).abs().compareTo(rule.changeValue) >= 0;
    }

    private boolean thresholdMatches(EnvironmentAnnouncementRule rule, BigDecimal value) {
        return rule.thresholdValue != null && value.compareTo(rule.thresholdValue) >= 0;
    }

    private EnvironmentOutdoorReading previousReading(EnvironmentOutdoorReading reading) {
        return readings.selectOne(new LambdaQueryWrapper<EnvironmentOutdoorReading>().eq(EnvironmentOutdoorReading::getSpaceId, reading.getSpaceId())
                .lt(reading.getId() != null, EnvironmentOutdoorReading::getId, reading.getId())
                .orderByDesc(EnvironmentOutdoorReading::getObservedAt).last("LIMIT 1"));
    }

    private boolean confirmed(EnvironmentAnnouncementRule rule) {
        int required = rule.consecutiveCount == null ? 2 : rule.consecutiveCount;
        if (required <= 1) return true;
        List<EnvironmentAnnouncementEvent> prior = events.selectList(new LambdaQueryWrapper<EnvironmentAnnouncementEvent>().eq(EnvironmentAnnouncementEvent::getRuleId, rule.id)
                .orderByDesc(EnvironmentAnnouncementEvent::getCreatedAt).last("LIMIT " + (required - 1)));
        return prior.size() == required - 1 && prior.stream().noneMatch(e -> "CLEAR".equals(e.status));
    }

    private boolean inQuietPeriod(EnvironmentAnnouncementRule rule, LocalTime now) {
        if (rule.quietStart == null || rule.quietEnd == null || rule.quietStart.equals(rule.quietEnd)) return false;
        return rule.quietStart.isBefore(rule.quietEnd) ? !now.isBefore(rule.quietStart) && now.isBefore(rule.quietEnd)
                : !now.isBefore(rule.quietStart) || now.isBefore(rule.quietEnd);
    }

    private boolean inCooldown(EnvironmentAnnouncementRule rule) {
        EnvironmentAnnouncementEvent last = events.selectOne(new LambdaQueryWrapper<EnvironmentAnnouncementEvent>().eq(EnvironmentAnnouncementEvent::getRuleId, rule.id)
                .eq(EnvironmentAnnouncementEvent::getStatus, "PUBLISHED").orderByDesc(EnvironmentAnnouncementEvent::getCreatedAt).last("LIMIT 1"));
        return last != null && last.createdAt.plusMinutes(rule.cooldownMinutes == null ? 60 : rule.cooldownMinutes).isAfter(LocalDateTime.now());
    }

    private String indoorMessage(String metric, BigDecimal value) {
        String number = value.stripTrailingZeros().toPlainString();
        return switch (metric) {
            case "indoor_temperature" -> "室内温度为" + number + "摄氏度，建议开启空调或通风。";
            case "indoor_humidity" -> "室内湿度为" + number + "% ，建议除湿或适当通风。";
            case "indoor_pressure" -> "室内气压为" + number + "百帕，请留意天气变化。";
            default -> "室内光照为" + number + "勒克斯，建议根据需要开灯或调节遮阳。";
        };
    }

    private String outdoorMessage(String metric, BigDecimal value) {
        String number = value.stripTrailingZeros().toPlainString();
        if ("precipitation".equals(metric)) return "室外近一小时降水为" + number + "毫米，正在下雨或有降水，出门请携带雨具。";
        if ("aqi".equals(metric)) return "室外空气质量指数为" + number + "，请酌情减少户外活动。";
        return "室外温度为" + number + "摄氏度，请注意防暑或保暖。";
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
