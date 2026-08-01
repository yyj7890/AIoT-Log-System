CREATE TABLE environment_announcement_rules (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  space_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  metric VARCHAR(32) NOT NULL COMMENT 'temperature、aqi、weather_warning',
  change_value DECIMAL(10,2) NULL COMMENT '达到该变化幅度才触发',
  threshold_value DECIMAL(10,2) NULL COMMENT '绝对阈值',
  consecutive_count INT NOT NULL DEFAULT 2 COMMENT '连续命中次数',
  cooldown_minutes INT NOT NULL DEFAULT 60,
  quiet_start TIME NULL,
  quiet_end TIME NULL,
  bypass_quiet_for_severe TINYINT(1) NOT NULL DEFAULT 1,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_environment_announcement_rules_space FOREIGN KEY (space_id) REFERENCES environment_spaces(id) ON DELETE CASCADE,
  INDEX idx_environment_announcement_rules_space_enabled (space_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='环境主动播报规则';

CREATE TABLE environment_announcement_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_id BIGINT NOT NULL,
  space_id BIGINT NOT NULL,
  event_key VARCHAR(160) NOT NULL COMMENT '用于连续确认和冷却去重',
  status VARCHAR(32) NOT NULL COMMENT 'OBSERVED、CONFIRMED、SUPPRESSED、PUBLISHED、FAILED',
  observed_value DECIMAL(10,2) NULL,
  message VARCHAR(300) NOT NULL,
  announcement_task_id VARCHAR(80) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_environment_announcement_events_rule FOREIGN KEY (rule_id) REFERENCES environment_announcement_rules(id) ON DELETE CASCADE,
  CONSTRAINT fk_environment_announcement_events_space FOREIGN KEY (space_id) REFERENCES environment_spaces(id) ON DELETE CASCADE,
  INDEX idx_environment_announcement_events_rule_created (rule_id, created_at),
  INDEX idx_environment_announcement_events_space_created (space_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='环境播报事件与交付关联';
