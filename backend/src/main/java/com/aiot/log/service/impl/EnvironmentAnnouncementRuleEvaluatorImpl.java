package com.aiot.log.service.impl;

import com.aiot.log.announcement.TextAnnouncementService;
import com.aiot.log.config.AnnouncementProperties;
import com.aiot.log.entity.*;
import com.aiot.log.mapper.*;
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
            EnvironmentOutdoorReadingMapper readings,
            EnvironmentSpaceMapper spaces, DeviceMapper devices, TextAnnouncementService announcements,
            AnnouncementProperties announcementProperties) {
        this.rules=rules; this.events=events; this.readings=readings; this.spaces=spaces; this.devices=devices; this.announcements=announcements; this.announcementProperties=announcementProperties;
    }
    @Override public void evaluate(EnvironmentOutdoorReading reading) {
        EnvironmentSpace space = spaces.selectById(reading.spaceId);
        if (space == null || !Boolean.TRUE.equals(space.getEnabled())) return;
        for (EnvironmentAnnouncementRule rule : rules.selectList(new LambdaQueryWrapper<EnvironmentAnnouncementRule>().eq(EnvironmentAnnouncementRule::getSpaceId, reading.spaceId).eq(EnvironmentAnnouncementRule::getEnabled, true))) {
            BigDecimal value = valueOf(reading, rule.metric);
            if (value == null) continue;
            if (!matches(rule, reading, value)) {
                EnvironmentAnnouncementEvent clear = event(rule, reading, value);
                clear.status = "CLEAR";
                events.insert(clear);
                continue;
            }
            EnvironmentAnnouncementEvent event = event(rule, reading, value);
            if (!confirmed(rule)) { event.status="OBSERVED"; events.insert(event); continue; }
            if (inQuietPeriod(rule, LocalTime.now()) || inCooldown(rule) || !announcementProperties.getTts().isEnabled()) { event.status="SUPPRESSED"; events.insert(event); continue; }
            if (space.getPrimarySpeakerDeviceId() == null) { event.status="SUPPRESSED"; events.insert(event); continue; }
            Device speaker = devices.selectById(space.getPrimarySpeakerDeviceId());
            if (speaker == null) { event.status="SUPPRESSED"; events.insert(event); continue; }
            try { event.announcementTaskId=announcements.publish(speaker.getDeviceCode(), event.message); event.status="PUBLISHED"; }
            catch (RuntimeException exception) { event.status="FAILED"; }
            events.insert(event);
        }
    }
    private EnvironmentAnnouncementEvent event(EnvironmentAnnouncementRule rule, EnvironmentOutdoorReading reading, BigDecimal value) {
        EnvironmentAnnouncementEvent event = new EnvironmentAnnouncementEvent(); event.ruleId=rule.id; event.spaceId=reading.spaceId;
        event.eventKey=rule.id+":"+reading.getId(); event.observedValue=value;
        event.message=rule.name+"，当前"+("aqi".equals(rule.metric)?"空气质量指数 ":"温度 ")+value.stripTrailingZeros().toPlainString(); event.createdAt=LocalDateTime.now(); return event;
    }
    private BigDecimal valueOf(EnvironmentOutdoorReading reading, String metric) { return "aqi".equals(metric) && reading.aqi != null ? BigDecimal.valueOf(reading.aqi) : "temperature".equals(metric) ? reading.temperature : null; }
    private boolean matches(EnvironmentAnnouncementRule rule, EnvironmentOutdoorReading reading, BigDecimal value) {
        boolean threshold = rule.thresholdValue != null && value.compareTo(rule.thresholdValue) >= 0;
        if (rule.changeValue == null) return threshold;
        EnvironmentOutdoorReading previous = readings.selectOne(new LambdaQueryWrapper<EnvironmentOutdoorReading>().eq(EnvironmentOutdoorReading::getSpaceId, reading.spaceId)
                .lt(reading.getId() != null, EnvironmentOutdoorReading::getId, reading.getId()).orderByDesc(EnvironmentOutdoorReading::getObservedAt).last("LIMIT 1"));
        BigDecimal prior = previous == null ? null : valueOf(previous, rule.metric);
        boolean changed = prior != null && value.subtract(prior).abs().compareTo(rule.changeValue) >= 0;
        return threshold || changed;
    }
    private boolean confirmed(EnvironmentAnnouncementRule rule) {
        int required = rule.consecutiveCount == null ? 2 : rule.consecutiveCount;
        if (required <= 1) return true;
        List<EnvironmentAnnouncementEvent> prior = events.selectList(new LambdaQueryWrapper<EnvironmentAnnouncementEvent>().eq(EnvironmentAnnouncementEvent::getRuleId, rule.id)
                .orderByDesc(EnvironmentAnnouncementEvent::getCreatedAt).last("LIMIT " + (required - 1)));
        return prior.size() == required - 1 && prior.stream().noneMatch(event -> "CLEAR".equals(event.status));
    }
    private boolean inQuietPeriod(EnvironmentAnnouncementRule rule, LocalTime now) {
        if (rule.quietStart == null || rule.quietEnd == null || rule.quietStart.equals(rule.quietEnd)) return false;
        return rule.quietStart.isBefore(rule.quietEnd) ? !now.isBefore(rule.quietStart) && now.isBefore(rule.quietEnd)
                : !now.isBefore(rule.quietStart) || now.isBefore(rule.quietEnd);
    }
    private boolean inCooldown(EnvironmentAnnouncementRule rule) {
        EnvironmentAnnouncementEvent last = events.selectOne(new LambdaQueryWrapper<EnvironmentAnnouncementEvent>().eq(EnvironmentAnnouncementEvent::getRuleId,rule.id).eq(EnvironmentAnnouncementEvent::getStatus,"PUBLISHED").orderByDesc(EnvironmentAnnouncementEvent::getCreatedAt).last("LIMIT 1"));
        return last != null && last.createdAt.plusMinutes(rule.cooldownMinutes == null ? 60 : rule.cooldownMinutes).isAfter(LocalDateTime.now());
    }
}
