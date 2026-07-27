package com.aiot.log.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class ReminderRequest {
    @NotBlank @Size(max = 80) private String requestId;
    @NotBlank @Size(max = 100) private String deviceCode;
    @NotBlank @Size(max = 500) private String message;
    @NotNull @Future private LocalDateTime remindAt;
    public String getRequestId() { return requestId; } public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getDeviceCode() { return deviceCode; } public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
    public String getMessage() { return message; } public void setMessage(String message) { this.message = message; }
    public LocalDateTime getRemindAt() { return remindAt; } public void setRemindAt(LocalDateTime remindAt) { this.remindAt = remindAt; }
}
