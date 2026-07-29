package com.aiot.log.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DeviceReportVO {

    private Long id;
    private Long deviceId;
    private String deviceName;
    private String deviceCode;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private BigDecimal voltage;
    private BigDecimal batteryPercent;
    private Boolean charging;
    private Integer signalStrength;
    private String status;
    private String message;
    private Boolean abnormal;
    private Long generatedLogId;
    private LocalDateTime reportedAt;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public BigDecimal getHumidity() {
        return humidity;
    }

    public void setHumidity(BigDecimal humidity) {
        this.humidity = humidity;
    }

    public BigDecimal getVoltage() {
        return voltage;
    }

    public void setVoltage(BigDecimal voltage) {
        this.voltage = voltage;
    }
    public BigDecimal getBatteryPercent() { return batteryPercent; }
    public void setBatteryPercent(BigDecimal batteryPercent) { this.batteryPercent = batteryPercent; }
    public Boolean getCharging() { return charging; }
    public void setCharging(Boolean charging) { this.charging = charging; }

    public Integer getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(Integer signalStrength) {
        this.signalStrength = signalStrength;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getAbnormal() {
        return abnormal;
    }

    public void setAbnormal(Boolean abnormal) {
        this.abnormal = abnormal;
    }

    public Long getGeneratedLogId() {
        return generatedLogId;
    }

    public void setGeneratedLogId(Long generatedLogId) {
        this.generatedLogId = generatedLogId;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
