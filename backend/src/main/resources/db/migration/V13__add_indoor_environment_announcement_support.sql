ALTER TABLE environment_spaces
  ADD COLUMN indoor_sensor_device_id BIGINT NULL COMMENT '室内环境传感器设备' AFTER primary_speaker_device_id,
  ADD CONSTRAINT fk_environment_spaces_indoor_sensor FOREIGN KEY (indoor_sensor_device_id) REFERENCES devices(id) ON DELETE SET NULL,
  ADD INDEX idx_environment_spaces_indoor_sensor (indoor_sensor_device_id);
