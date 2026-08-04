package com.aiot.log.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("device_reports")
public class DeviceReport {

    private Long id;
    private Long deviceId;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private BigDecimal pressure;
    private BigDecimal illuminance;
    private BigDecimal voltage;
    private BigDecimal batteryPercent;
    private Boolean charging;
    private Integer signalStrength;
    private String status;
    private String message;
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

    public BigDecimal getPressure() { return pressure; }
    public void setPressure(BigDecimal pressure) { this.pressure = pressure; }
    public BigDecimal getIlluminance() { return illuminance; }
    public void setIlluminance(BigDecimal illuminance) { this.illuminance = illuminance; }

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
