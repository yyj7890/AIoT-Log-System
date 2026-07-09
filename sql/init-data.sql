SET NAMES utf8mb4;

USE aiot_log_system;

INSERT INTO devices (name, device_code, type, location, status, description, last_online_at)
VALUES
  ('AI 小智语音助手', 'XIAOZHI-001', 'AIoT 智能语音终端', '实验室', 'NORMAL', '用于语音交互、设备控制和物联网实验的智能终端', NOW()),
  ('实验室温湿度采集节点', 'TEMP-HUM-001', '环境采集设备', '实验室', 'NORMAL', '用于采集实验室温度和湿度数据', NOW()),
  ('教室环境监测终端', 'CLASSROOM-ENV-001', '环境监测终端', '教室', 'ABNORMAL', '用于监测教室环境状态', DATE_SUB(NOW(), INTERVAL 2 HOUR));

INSERT INTO tags (name)
VALUES
  ('异常'),
  ('维修'),
  ('实验'),
  ('告警'),
  ('已解决'),
  ('网络'),
  ('语音模块'),
  ('传感器');

INSERT INTO logs (device_id, title, content, log_type, level, status, source, created_at)
VALUES
  (1, '设备启动成功', 'AI 小智设备正常启动，WiFi 连接成功，语音识别模块启动成功。', 'RUNNING', 'INFO', 'RESOLVED', 'MANUAL', DATE_SUB(NOW(), INTERVAL 7 DAY)),
  (1, '语音唤醒失败', '设备多次无法响应唤醒词，需要检查麦克风模块和语音识别服务。', 'ERROR', 'ERROR', 'PENDING', 'MANUAL', DATE_SUB(NOW(), INTERVAL 5 DAY)),
  (1, '更换麦克风模块', '更换麦克风模块后，语音识别功能恢复正常。', 'MAINTENANCE', 'INFO', 'RESOLVED', 'MANUAL', DATE_SUB(NOW(), INTERVAL 4 DAY)),
  (1, '设备巡检正常', '检查设备电源、网络和语音模块状态，运行正常。', 'INSPECTION', 'INFO', 'RESOLVED', 'MANUAL', DATE_SUB(NOW(), INTERVAL 3 DAY)),
  (2, '温湿度采集正常', '温湿度采集节点运行正常，数据采集稳定。', 'RUNNING', 'INFO', 'RESOLVED', 'MANUAL', DATE_SUB(NOW(), INTERVAL 2 DAY)),
  (2, '传感器接口清理', '清理传感器接口和设备外壳，设备运行状态正常。', 'MAINTENANCE', 'INFO', 'RESOLVED', 'MANUAL', DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (3, '设备短暂离线', '教室环境监测终端出现短暂离线，疑似网络信号不稳定。', 'ERROR', 'WARNING', 'PROCESSING', 'MANUAL', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
  (3, '检查安装位置和供电状态', '检查设备安装位置、供电状态和网络连接，发现网络信号较弱。', 'INSPECTION', 'WARNING', 'PROCESSING', 'MANUAL', DATE_SUB(NOW(), INTERVAL 6 HOUR));

INSERT INTO log_tags (log_id, tag_id)
VALUES
  (2, 1),
  (2, 4),
  (2, 7),
  (3, 2),
  (3, 5),
  (5, 3),
  (6, 2),
  (6, 8),
  (7, 1),
  (7, 4),
  (7, 6),
  (8, 6);
