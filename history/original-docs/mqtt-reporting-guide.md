# MQTT 设备上报说明

本文档说明真实设备如何通过 MQTT 向系统上报数据。当前项目已经完成 MQTT 首版链路：

```text
设备 MQTT Client -> Mosquitto Broker -> Spring Boot 后端订阅 -> device_reports 入库 -> 异常时生成 DEVICE 日志 -> 前端展示
```

## 1. 当前完成状态

已完成：

- 后端 MQTT 订阅：`aiot/device/+/report`
- 本地 Broker：Mosquitto，默认端口 `1883`
- MQTT 消息解析后复用 HTTP 上报逻辑
- 正常上报写入 `device_reports`
- 异常上报自动生成来源为 `DEVICE` 的日志
- 模拟发布脚本：`tools/simulate-mqtt-report.ps1`
- MQTT 状态接口：`GET /api/mqtt/status`
- 前端 MQTT 状态页面：`/mqtt`

## 2. 真实设备怎么连接

真实设备不需要运行项目里的 PowerShell 脚本。脚本只是模拟设备行为。

真实设备需要具备 MQTT Client 能力，然后按下面参数连接：

| 项目 | 当前值 |
| --- | --- |
| Broker 地址 | `127.0.0.1` 或服务器 IP |
| Broker 端口 | `1883` |
| 协议 | MQTT TCP |
| Topic | `aiot/device/{deviceCode}/report` |
| QoS | `1` |
| 用户名/密码 | 当前本地开发版未配置 |
| Payload | JSON |

如果设备程序和后端/Mosquitto 在同一台电脑上，可以使用：

```text
127.0.0.1:1883
```

如果是真实硬件、另一台电脑或开发板连接你的电脑，需要使用你电脑在局域网里的 IP，例如：

```text
192.168.1.20:1883
```

这时 Mosquitto 也必须允许局域网访问，不能只监听本机。

## 3. 上报 Topic

设备上报 Topic 格式：

```text
aiot/device/{deviceCode}/report
```

示例：

```text
aiot/device/TEMP-HUM-001/report
```

其中 `TEMP-HUM-001` 必须是系统里已经存在的设备编号。

## 4. 上报 JSON 格式

正常上报示例：

```json
{
  "deviceCode": "TEMP-HUM-001",
  "temperature": 28,
  "humidity": 55,
  "voltage": 222,
  "signalStrength": -70,
  "status": "NORMAL",
  "message": "device normal report",
  "reportedAt": "2026-07-08T12:48:54"
}
```

异常上报示例：

```json
{
  "deviceCode": "TEMP-HUM-001",
  "temperature": 90,
  "humidity": 65,
  "voltage": 198,
  "signalStrength": -99,
  "status": "ABNORMAL",
  "message": "device abnormal report",
  "reportedAt": "2026-07-08T12:48:54"
}
```

字段说明：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `deviceCode` | 是 | 设备编号，必须能匹配 `devices.device_code` |
| `temperature` | 否 | 温度 |
| `humidity` | 否 | 湿度 |
| `voltage` | 否 | 电压 |
| `signalStrength` | 否 | 信号强度，单位 dBm |
| `status` | 否 | `NORMAL` 或 `ABNORMAL`，不传默认按正常处理 |
| `message` | 否 | 上报说明 |
| `reportedAt` | 否 | 设备上报时间，不传则使用后端接收时间 |

## 5. Mosquitto 本地和局域网访问区别

Mosquitto 2.x 默认可能以 local only 模式运行，只允许本机客户端连接。此时：

- 本机脚本可以连接 `127.0.0.1:1883`
- 其他设备无法连接这台电脑的 `1883`

如果要让真实设备从局域网连接，需要配置 Mosquitto 监听局域网地址。开发环境可创建一个配置文件，例如：

```conf
listener 1883 0.0.0.0
allow_anonymous true
```

然后用该配置启动 Mosquitto。注意：`allow_anonymous true` 只适合本地开发和课堂演示，正式环境应配置用户名、密码、防火墙和访问控制。

还需要确认 Windows 防火墙允许 TCP `1883` 端口入站访问。

## 6. 模拟脚本和真实设备的关系

项目脚本：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File D:\AI\IOT\tools\simulate-mqtt-report.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File D:\AI\IOT\tools\simulate-mqtt-report.ps1 -Abnormal
```

等价于真实设备做了这件事：

```text
连接 MQTT Broker -> 发布 JSON 到 aiot/device/TEMP-HUM-001/report
```

所以脚本不是正式功能本身，只是用来验证 MQTT 链路。

## 7. 排查顺序

如果设备上报没有出现在页面上，按这个顺序查：

1. MQTT 状态页面 `/mqtt` 是否显示已连接。
2. Broker 地址和端口是否正确。
3. 设备发布的 topic 是否是 `aiot/device/{deviceCode}/report`。
4. JSON 是否是合法格式，字段名是否正确。
5. `deviceCode` 是否已经存在于设备管理。
6. 如果设备不在本机，Mosquitto 是否允许局域网访问，Windows 防火墙是否放行 `1883`。
7. 后端日志里是否有 `MQTT device report ignored`。
