ALTER TABLE reminders
  ADD COLUMN request_id VARCHAR(80) NULL COMMENT '调用方幂等请求标识' AFTER device_id,
  ADD CONSTRAINT uk_reminders_request_id UNIQUE (request_id);
