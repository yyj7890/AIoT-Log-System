package com.aiot.log.vo;

import java.time.LocalDateTime;

public class DeviceComponentVO {
    private Long id; private Long deviceId; private String name; private String category; private String model;
    private Integer quantity; private String notes; private String source; private LocalDateTime updatedAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getDeviceId() { return deviceId; } public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getCategory() { return category; } public void setCategory(String category) { this.category = category; }
    public String getModel() { return model; } public void setModel(String model) { this.model = model; }
    public Integer getQuantity() { return quantity; } public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getNotes() { return notes; } public void setNotes(String notes) { this.notes = notes; }
    public String getSource() { return source; } public void setSource(String source) { this.source = source; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
