CREATE TABLE environment_outdoor_readings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  space_id BIGINT NOT NULL,
  temperature DECIMAL(6,2) NULL,
  humidity DECIMAL(6,2) NULL,
  weather_text VARCHAR(80) NULL,
  aqi INT NULL,
  pm2p5 DECIMAL(8,2) NULL,
  primary_pollutant VARCHAR(80) NULL,
  observed_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_environment_outdoor_readings_space FOREIGN KEY (space_id) REFERENCES environment_spaces(id) ON DELETE CASCADE,
  INDEX idx_environment_outdoor_readings_space_observed (space_id, observed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='室外天气和空气质量读数';
