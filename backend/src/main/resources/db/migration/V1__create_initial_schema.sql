CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL COMMENT '用户名',
  password VARCHAR(255) NOT NULL COMMENT '密码',
  nickname VARCHAR(50) NULL COMMENT '昵称',
  role VARCHAR(30) NOT NULL DEFAULT 'USER' COMMENT '角色',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE devices (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT '设备名称',
  device_code VARCHAR(100) NOT NULL COMMENT '设备编号',
  type VARCHAR(100) NOT NULL COMMENT '设备类型',
  location VARCHAR(255) NULL COMMENT '安装位置',
  status VARCHAR(30) NOT NULL DEFAULT 'NORMAL' COMMENT '设备状态：NORMAL正常，ABNORMAL异常，OFFLINE离线，MAINTENANCE维护中',
  description TEXT NULL COMMENT '设备描述',
  last_online_at DATETIME NULL COMMENT '最后在线时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT uk_devices_device_code UNIQUE (device_code),
  INDEX idx_devices_status (status),
  INDEX idx_devices_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备表';

CREATE TABLE logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_id BIGINT NOT NULL COMMENT '设备ID',
  title VARCHAR(200) NOT NULL COMMENT '日志标题',
  content TEXT NOT NULL COMMENT '日志内容',
  log_type VARCHAR(30) NOT NULL COMMENT '日志类型：RUNNING运行，ERROR异常，MAINTENANCE维护，INSPECTION巡检',
  level VARCHAR(30) NOT NULL DEFAULT 'INFO' COMMENT '日志等级：INFO普通，WARNING警告，ERROR严重',
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING待处理，PROCESSING处理中，RESOLVED已解决',
  source VARCHAR(30) NOT NULL DEFAULT 'MANUAL' COMMENT '日志来源：MANUAL手动录入，DEVICE设备上报，SYSTEM系统生成',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_logs_device_id FOREIGN KEY (device_id) REFERENCES devices(id),
  INDEX idx_logs_device_id (device_id),
  INDEX idx_logs_log_type (log_type),
  INDEX idx_logs_status (status),
  INDEX idx_logs_level (level),
  INDEX idx_logs_created_at (created_at),
  INDEX idx_logs_device_created (device_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日志表';

CREATE TABLE tags (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL COMMENT '标签名称',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT uk_tags_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签表';

CREATE TABLE log_tags (
  log_id BIGINT NOT NULL COMMENT '日志ID',
  tag_id BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (log_id, tag_id),
  CONSTRAINT fk_log_tags_log_id FOREIGN KEY (log_id) REFERENCES logs(id) ON DELETE CASCADE,
  CONSTRAINT fk_log_tags_tag_id FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日志标签关联表';

CREATE TABLE device_reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_id BIGINT NOT NULL COMMENT '设备ID',
  temperature DECIMAL(8,2) NULL COMMENT '温度，单位摄氏度',
  humidity DECIMAL(8,2) NULL COMMENT '湿度，单位百分比',
  voltage DECIMAL(8,2) NULL COMMENT '电压，单位伏',
  signal_strength INT NULL COMMENT '信号强度，单位dBm',
  status VARCHAR(30) NOT NULL DEFAULT 'NORMAL' COMMENT '设备上报状态',
  message VARCHAR(500) NULL COMMENT '设备上报说明',
  reported_at DATETIME NOT NULL COMMENT '设备上报时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  CONSTRAINT fk_device_reports_device_id FOREIGN KEY (device_id) REFERENCES devices(id),
  INDEX idx_device_reports_device_id (device_id),
  INDEX idx_device_reports_reported_at (reported_at),
  INDEX idx_device_reports_status (status),
  INDEX idx_device_reports_device_reported (device_id, reported_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备上报数据表';

CREATE TABLE alert_rules (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT '规则名称',
  device_id BIGINT NULL COMMENT '设备ID，空表示全局规则',
  metric VARCHAR(30) NOT NULL COMMENT '指标：temperature, humidity, voltage, signalStrength',
  operator VARCHAR(10) NOT NULL COMMENT '比较符：GT大于，LT小于，GTE大于等于，LTE小于等于，EQ等于',
  threshold_value DECIMAL(10,2) NOT NULL COMMENT '阈值',
  level VARCHAR(30) NOT NULL DEFAULT 'WARNING' COMMENT '告警等级',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_alert_rules_device_id FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE,
  INDEX idx_alert_rules_device_id (device_id),
  INDEX idx_alert_rules_enabled (enabled),
  INDEX idx_alert_rules_metric (metric)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警规则表';
