CREATE TABLE reminders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_id BIGINT NOT NULL,
  device_code VARCHAR(100) NOT NULL,
  message VARCHAR(500) NOT NULL,
  remind_at DATETIME NOT NULL,
  status VARCHAR(32) NOT NULL COMMENT 'SCHEDULED、TRIGGERING、PUBLISHED、FAILED、CANCELED',
  delivery_task_id VARCHAR(80) NULL,
  triggered_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_reminders_device_id FOREIGN KEY (device_id) REFERENCES devices(id),
  INDEX idx_reminders_status_time (status, remind_at),
  INDEX idx_reminders_device_time (device_id, remind_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='一次性设备提醒';
