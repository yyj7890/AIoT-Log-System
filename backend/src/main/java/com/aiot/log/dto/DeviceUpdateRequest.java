package com.aiot.log.dto;

import jakarta.validation.constraints.NotBlank;

public class DeviceUpdateRequest {

    @NotBlank(message = "设备名称不能为空")
    private String name;

    @NotBlank(message = "设备类型不能为空")
    private String type;
    private String manufacturer;
    private String model;
    private String serialNumber;
    private String firmwareVersion;

    private String monitoringMode;
    private String location;
    private String status;
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }

    public String getMonitoringMode() {
        return monitoringMode;
    }

    public void setMonitoringMode(String monitoringMode) {
        this.monitoringMode = monitoringMode;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
