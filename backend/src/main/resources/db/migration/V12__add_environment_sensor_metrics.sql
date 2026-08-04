ALTER TABLE device_reports
  ADD COLUMN pressure DECIMAL(8,2) NULL COMMENT '气压，单位 hPa' AFTER humidity,
  ADD COLUMN illuminance DECIMAL(10,2) NULL COMMENT '光照度，单位 lux' AFTER pressure;
