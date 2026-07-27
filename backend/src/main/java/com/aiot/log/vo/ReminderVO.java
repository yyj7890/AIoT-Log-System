package com.aiot.log.vo;

import java.time.LocalDateTime;

public record ReminderVO(Long id, String requestId, String deviceCode, String message, LocalDateTime remindAt,
                         String status, String deliveryTaskId, LocalDateTime triggeredAt) {
}
