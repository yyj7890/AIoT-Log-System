package com.aiot.log.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DeviceReportCreateRequest {

    @NotBlank(message = "设备编号不能为空")
    private String deviceCode;

    @DecimalMin(value = "-50.0", message = "温度不能低于 -50")
    @DecimalMax(value = "150.0", message = "温度不能高于 150")
    private BigDecimal temperature;

    @DecimalMin(value = "0.0", message = "湿度不能低于 0")
    @DecimalMax(value = "100.0", message = "湿度不能高于 100")
    private BigDecimal humidity;

    @DecimalMin(value = "0.0", message = "电压不能低于 0")
    @DecimalMax(value = "1000.0", message = "电压不能高于 1000")
    private BigDecimal voltage;

    @DecimalMin(value = "0.0", message = "电量不能低于 0")
    @DecimalMax(value = "100.0", message = "电量不能高于 100")
    private BigDecimal batteryPercent;

    private Boolean charging;

    @Min(value = -120, message = "信号强度不能低于 -120")
    @Max(value = 0, message = "信号强度不能高于 0")
    private Integer signalStrength;

    private String status;

    @Size(max = 500, message = "上报说明不能超过 500 个字符")
    private String message;

    private LocalDateTime reportedAt;

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

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }
}
