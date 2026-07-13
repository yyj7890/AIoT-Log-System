SET NAMES utf8mb4;

UPDATE logs
SET title = CASE title
    WHEN '设备运行事件：startup' THEN '设备启动'
    WHEN '设备运行事件：firmware_started' THEN '固件启动'
    WHEN '设备运行事件：wifi_connected' THEN 'Wi-Fi 已连接'
    WHEN '设备运行事件：mqtt_connected' THEN '日志 MQTT 已连接'
    WHEN '设备运行事件：official_protocol_connected' THEN '官方 AI 协议已连接'
    ELSE title
END,
content = CASE content
    WHEN 'Firmware initialization started' THEN '固件开始初始化'
    WHEN 'Firmware initialization completed' THEN '固件初始化完成'
    WHEN 'Wi-Fi connected' THEN 'Wi-Fi 已连接'
    WHEN 'Log MQTT connected' THEN '日志 MQTT 已连接'
    WHEN 'Official AI protocol connected or reconnected' THEN '官方 AI 协议已连接或重连'
    ELSE content
END
WHERE source = 'DEVICE'
  AND (
    title IN (
      '设备运行事件：startup',
      '设备运行事件：firmware_started',
      '设备运行事件：wifi_connected',
      '设备运行事件：mqtt_connected',
      '设备运行事件：official_protocol_connected'
    )
    OR content IN (
      'Firmware initialization started',
      'Firmware initialization completed',
      'Wi-Fi connected',
      'Log MQTT connected',
      'Official AI protocol connected or reconnected'
    )
  );
