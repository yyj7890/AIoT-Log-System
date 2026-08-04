package com.aiot.log.service;
import com.aiot.log.entity.EnvironmentOutdoorReading;
import com.aiot.log.entity.DeviceReport;
public interface EnvironmentAnnouncementRuleEvaluator { void evaluate(EnvironmentOutdoorReading reading); void evaluateIndoor(DeviceReport report); }
