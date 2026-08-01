CREATE TABLE environment_spaces (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT '环境空间名称',
  address VARCHAR(255) NULL COMMENT '展示地址，可精确到门牌/房间',
  latitude DECIMAL(9,6) NULL COMMENT '纬度，仅供后端天气查询',
  longitude DECIMAL(9,6) NULL COMMENT '经度，仅供后端天气查询',
  primary_speaker_device_id BIGINT NULL COMMENT '主播报小智设备',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_environment_spaces_speaker FOREIGN KEY (primary_speaker_device_id) REFERENCES devices(id) ON DELETE SET NULL,
  INDEX idx_environment_spaces_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='环境监测空间';
