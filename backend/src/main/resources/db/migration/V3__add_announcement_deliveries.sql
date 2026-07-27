CREATE TABLE announcement_deliveries (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(80) NOT NULL COMMENT '播报任务唯一编号',
  device_id BIGINT NOT NULL COMMENT '目标设备ID',
  device_code VARCHAR(100) NOT NULL COMMENT '目标设备编号快照',
  priority INT NOT NULL COMMENT '播报优先级',
  expires_at DATETIME NOT NULL COMMENT '过期时间',
  status VARCHAR(32) NOT NULL COMMENT 'PUBLISHED、RECEIVED、PLAYED、FAILED',
  command_published_at DATETIME NULL COMMENT 'manifest发布时间',
  received_at DATETIME NULL COMMENT '设备接收确认时间',
  played_at DATETIME NULL COMMENT '设备播放完成时间',
  failed_at DATETIME NULL COMMENT '设备失败确认时间',
  failure_reason VARCHAR(200) NULL COMMENT '安全失败原因',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_announcement_deliveries_task_id UNIQUE (task_id),
  CONSTRAINT fk_announcement_deliveries_device_id FOREIGN KEY (device_id) REFERENCES devices(id),
  INDEX idx_announcement_deliveries_device_created (device_id, created_at),
  INDEX idx_announcement_deliveries_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备主动播报任务';

CREATE TABLE announcement_delivery_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  delivery_id BIGINT NOT NULL COMMENT '播报任务ID',
  status VARCHAR(32) NOT NULL COMMENT 'RECEIVED、PLAYED、FAILED',
  reason VARCHAR(200) NULL COMMENT '安全失败原因',
  reported_at DATETIME NOT NULL COMMENT '设备回执时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_announcement_delivery_events_delivery_status UNIQUE (delivery_id, status),
  CONSTRAINT fk_announcement_delivery_events_delivery_id FOREIGN KEY (delivery_id) REFERENCES announcement_deliveries(id) ON DELETE CASCADE,
  INDEX idx_announcement_delivery_events_delivery_reported (delivery_id, reported_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备主动播报回执时间线';
