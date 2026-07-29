ALTER TABLE devices
  ADD COLUMN manufacturer VARCHAR(100) NULL COMMENT '厂商' AFTER type,
  ADD COLUMN model VARCHAR(100) NULL COMMENT '型号' AFTER manufacturer,
  ADD COLUMN serial_number VARCHAR(100) NULL COMMENT '序列号' AFTER model,
  ADD COLUMN firmware_version VARCHAR(100) NULL COMMENT '固件版本' AFTER serial_number;

ALTER TABLE device_reports
  ADD COLUMN battery_percent DECIMAL(5,2) NULL COMMENT '电量百分比' AFTER voltage,
  ADD COLUMN charging TINYINT(1) NULL COMMENT '是否充电中' AFTER battery_percent;

CREATE TABLE device_components (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL COMMENT '零件名称',
  category VARCHAR(50) NULL COMMENT '零件类别',
  model VARCHAR(100) NULL COMMENT '型号或规格',
  quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
  notes VARCHAR(500) NULL COMMENT '备注',
  source VARCHAR(30) NOT NULL DEFAULT 'MANUAL' COMMENT '信息来源',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_device_components_device_id FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE,
  INDEX idx_device_components_device_id (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备零件清单';
