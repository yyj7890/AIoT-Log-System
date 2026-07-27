package com.aiot.log.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("reminders")
public class Reminder {
    private Long id;
    private Long deviceId;
    private String deviceCode;
    private String message;
    private LocalDateTime remindAt;
    private String status;
    private String deliveryTaskId;
    private LocalDateTime triggeredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getDeviceId() { return deviceId; } public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getDeviceCode() { return deviceCode; } public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
    public String getMessage() { return message; } public void setMessage(String message) { this.message = message; }
    public LocalDateTime getRemindAt() { return remindAt; } public void setRemindAt(LocalDateTime remindAt) { this.remindAt = remindAt; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getDeliveryTaskId() { return deliveryTaskId; } public void setDeliveryTaskId(String deliveryTaskId) { this.deliveryTaskId = deliveryTaskId; }
    public LocalDateTime getTriggeredAt() { return triggeredAt; } public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
