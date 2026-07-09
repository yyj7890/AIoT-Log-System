# AIoT 智能设备运行日志管理系统 - 后端 REST API 设计

## 1. 设计目标

本文档定义第一阶段后端 REST API。

第一阶段后端需要支持：

- 首页统计
- 设备管理
- 日志管理
- 标签管理
- 日志筛选

后端推荐技术栈：

- Spring Boot
- MySQL
- MyBatis Plus 或 Spring Data JPA
- REST API

## 2. 通用约定

### 2.1 基础路径

```text
/api
```

### 2.2 请求格式

请求体使用 JSON：

```http
Content-Type: application/json
```

### 2.3 通用响应结构

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 业务状态码 |
| message | string | 响应消息 |
| data | any | 响应数据 |

### 2.4 分页响应结构

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "page": 1,
    "pageSize": 10
  }
}
```

### 2.5 常用状态码

| code | 说明 |
| --- | --- |
| 200 | 成功 |
| 400 | 请求参数错误 |
| 404 | 数据不存在 |
| 409 | 数据冲突，例如设备编号重复 |
| 500 | 服务器内部错误 |

## 3. 首页统计 API

### 3.1 获取首页统计数据

```http
GET /api/dashboard/summary
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "deviceTotal": 3,
    "normalDeviceCount": 2,
    "abnormalDeviceCount": 1,
    "offlineDeviceCount": 0,
    "maintenanceDeviceCount": 0,
    "pendingLogCount": 1,
    "recentErrorLogs": [],
    "recentMaintenanceLogs": []
  }
}
```

## 4. 设备 API

### 4.1 分页查询设备列表

```http
GET /api/devices
```

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | number | 否 | 页码，默认 1 |
| pageSize | number | 否 | 每页数量，默认 10 |
| keyword | string | 否 | 设备名称或设备编号关键词 |
| type | string | 否 | 设备类型 |
| status | string | 否 | 设备状态 |

响应数据字段：

```json
{
  "records": [
    {
      "id": 1,
      "name": "AI 小智语音助手",
      "deviceCode": "XIAOZHI-001",
      "type": "AIoT 智能语音终端",
      "location": "实验室",
      "status": "NORMAL",
      "lastOnlineAt": "2026-07-06 19:00:00",
      "createdAt": "2026-07-06 19:00:00",
      "updatedAt": "2026-07-06 19:00:00"
    }
  ],
  "total": 1,
  "page": 1,
  "pageSize": 10
}
```

### 4.2 获取设备详情

```http
GET /api/devices/{id}
```

响应数据：

```json
{
  "id": 1,
  "name": "AI 小智语音助手",
  "deviceCode": "XIAOZHI-001",
  "type": "AIoT 智能语音终端",
  "location": "实验室",
  "status": "NORMAL",
  "description": "用于语音交互、设备控制和物联网实验的智能终端",
  "lastOnlineAt": "2026-07-06 19:00:00",
  "logCount": 4,
  "errorLogCount": 1,
  "pendingLogCount": 1,
  "recentLogs": []
}
```

### 4.3 新增设备

```http
POST /api/devices
```

请求体：

```json
{
  "name": "AI 小智语音助手",
  "deviceCode": "XIAOZHI-001",
  "type": "AIoT 智能语音终端",
  "location": "实验室",
  "status": "NORMAL",
  "description": "用于语音交互、设备控制和物联网实验的智能终端"
}
```

校验规则：

- `name` 必填
- `deviceCode` 必填且唯一
- `type` 必填
- `status` 必须是合法设备状态

### 4.4 更新设备

```http
PUT /api/devices/{id}
```

请求体：

```json
{
  "name": "AI 小智语音助手",
  "type": "AIoT 智能语音终端",
  "location": "实验室",
  "status": "MAINTENANCE",
  "description": "正在维护麦克风模块"
}
```

说明：

- 第一阶段建议不修改 `deviceCode`。
- 如果后续允许修改设备编号，必须校验唯一性。

### 4.5 删除设备

```http
DELETE /api/devices/{id}
```

业务规则：

- 如果设备下存在日志，不允许删除。
- 如果设备下没有日志，允许删除。

存在日志时响应：

```json
{
  "code": 409,
  "message": "设备下存在日志，不能删除",
  "data": null
}
```

## 5. 日志 API

### 5.1 分页查询日志列表

```http
GET /api/logs
```

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | number | 否 | 页码，默认 1 |
| pageSize | number | 否 | 每页数量，默认 10 |
| deviceId | number | 否 | 设备 ID |
| logType | string | 否 | 日志类型 |
| level | string | 否 | 日志等级 |
| status | string | 否 | 处理状态 |
| source | string | 否 | 日志来源：MANUAL 人工录入，DEVICE 设备上报，SYSTEM 系统生成 |
| tagId | number | 否 | 标签 ID |
| keyword | string | 否 | 关键词 |
| startTime | string | 否 | 开始时间 |
| endTime | string | 否 | 结束时间 |

响应数据字段：

```json
{
  "records": [
    {
      "id": 1,
      "deviceId": 1,
      "deviceName": "AI 小智语音助手",
      "deviceCode": "XIAOZHI-001",
      "title": "设备启动成功",
      "content": "AI 小智设备正常启动，WiFi 连接成功。",
      "logType": "RUNNING",
      "level": "INFO",
      "status": "RESOLVED",
      "source": "MANUAL",
      "tags": [
        {
          "id": 1,
          "name": "实验"
        }
      ],
      "createdAt": "2026-07-06 19:00:00",
      "updatedAt": "2026-07-06 19:00:00"
    }
  ],
  "total": 1,
  "page": 1,
  "pageSize": 10
}
```

### 5.2 获取日志详情

```http
GET /api/logs/{id}
```

### 5.3 新增日志

```http
POST /api/logs
```

请求体：

```json
{
  "deviceId": 1,
  "title": "语音唤醒失败",
  "content": "设备多次无法响应唤醒词，需要检查麦克风模块。",
  "logType": "ERROR",
  "level": "ERROR",
  "status": "PENDING",
  "tagIds": [1, 4, 7]
}
```

校验规则：

- `deviceId` 必填，且设备必须存在
- `title` 必填
- `content` 必填
- `logType` 必须合法
- `level` 必须合法
- `status` 必须合法
- `tagIds` 中的标签必须存在

### 5.4 更新日志

```http
PUT /api/logs/{id}
```

请求体：

```json
{
  "title": "语音唤醒失败",
  "content": "已确认麦克风模块接触不良，正在处理。",
  "logType": "ERROR",
  "level": "ERROR",
  "status": "PROCESSING",
  "tagIds": [1, 4, 7]
}
```

### 5.5 修改日志状态

```http
PATCH /api/logs/{id}/status
```

请求体：

```json
{
  "status": "RESOLVED"
}
```

### 5.6 删除日志

```http
DELETE /api/logs/{id}
```

业务规则：

- 删除日志时同步删除日志标签关联关系。

## 6. 标签 API

### 6.1 查询标签列表

```http
GET /api/tags
```

响应数据：

```json
[
  {
    "id": 1,
    "name": "异常"
  },
  {
    "id": 2,
    "name": "维修"
  }
]
```

### 6.2 新增标签

```http
POST /api/tags
```

请求体：

```json
{
  "name": "网络"
}
```

校验规则：

- `name` 必填
- `name` 不能重复

### 6.3 删除标签

```http
DELETE /api/tags/{id}
```

业务规则：

- 删除标签时同步删除日志标签关联关系。

## 7. 枚举查询 API

为了前端下拉框统一管理，建议提供枚举接口。

```http
GET /api/enums
```

响应示例：

```json
{
  "deviceStatus": [
    { "label": "正常", "value": "NORMAL" },
    { "label": "异常", "value": "ABNORMAL" },
    { "label": "离线", "value": "OFFLINE" },
    { "label": "维护中", "value": "MAINTENANCE" }
  ],
  "logType": [
    { "label": "运行日志", "value": "RUNNING" },
    { "label": "异常日志", "value": "ERROR" },
    { "label": "维护日志", "value": "MAINTENANCE" },
    { "label": "巡检日志", "value": "INSPECTION" }
  ],
  "logLevel": [
    { "label": "普通", "value": "INFO" },
    { "label": "警告", "value": "WARNING" },
    { "label": "严重", "value": "ERROR" }
  ],
  "logStatus": [
    { "label": "待处理", "value": "PENDING" },
    { "label": "处理中", "value": "PROCESSING" },
    { "label": "已解决", "value": "RESOLVED" }
  ],
  "logSource": [
    { "label": "人工录入", "value": "MANUAL" },
    { "label": "设备上报", "value": "DEVICE" },
    { "label": "系统生成", "value": "SYSTEM" }
  ]
}
```

## 8. 设备上报 API

第二阶段新增设备自动上报接口，用于接收模拟设备或真实设备提交的传感器运行数据。

### 8.1 提交设备上报数据

```http
POST /api/device-reports
```

请求示例：

```json
{
  "deviceCode": "TEMP-HUM-001",
  "temperature": 28.5,
  "humidity": 56.2,
  "voltage": 224.0,
  "signalStrength": -68,
  "status": "NORMAL",
  "message": "模拟设备正常上报",
  "reportedAt": "2026-07-07T11:30:00"
}
```

业务规则：

- `deviceCode` 必填，并且必须匹配已存在设备。
- `status` 不传时默认为 `NORMAL`。
- 上报成功后写入 `device_reports`。
- 更新设备 `last_online_at`。
- 当状态为 `ABNORMAL`、温度大于 `80`、电压小于 `210` 或信号强度小于 `-95` 时，自动生成一条来源为 `DEVICE` 的异常日志。

### 8.2 查询设备上报数据

```http
GET /api/device-reports?page=1&pageSize=10&deviceId=2&status=NORMAL
```

## 9. 第一阶段接口清单

| 模块 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 首页 | GET | `/api/dashboard/summary` | 获取首页统计 |
| 设备 | GET | `/api/devices` | 分页查询设备 |
| 设备 | GET | `/api/devices/{id}` | 获取设备详情 |
| 设备 | POST | `/api/devices` | 新增设备 |
| 设备 | PUT | `/api/devices/{id}` | 更新设备 |
| 设备 | DELETE | `/api/devices/{id}` | 删除设备 |
| 日志 | GET | `/api/logs` | 分页查询日志 |
| 日志 | GET | `/api/logs/{id}` | 获取日志详情 |
| 日志 | POST | `/api/logs` | 新增日志 |
| 日志 | PUT | `/api/logs/{id}` | 更新日志 |
| 日志 | PATCH | `/api/logs/{id}/status` | 修改日志状态 |
| 日志 | DELETE | `/api/logs/{id}` | 删除日志 |
| 标签 | GET | `/api/tags` | 查询标签 |
| 标签 | POST | `/api/tags` | 新增标签 |
| 标签 | DELETE | `/api/tags/{id}` | 删除标签 |
| 枚举 | GET | `/api/enums` | 查询前端枚举 |

## 10. 第二阶段接口清单

| 模块 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 设备上报 | POST | `/api/device-reports` | 设备提交运行数据 |
| 设备上报 | GET | `/api/device-reports` | 分页查询设备上报数据 |
| 设备详情 | GET | `/api/devices/{id}` | 返回 `recentReports` 最近上报数据 |
| 日志 | GET | `/api/logs?source=DEVICE` | 按日志来源筛选设备上报日志 |
| 告警规则 | GET | `/api/alert-rules` | 查询告警规则 |
| 告警规则 | POST | `/api/alert-rules` | 新增告警规则 |
| 告警规则 | PUT | `/api/alert-rules/{id}` | 更新告警规则 |
| 告警规则 | DELETE | `/api/alert-rules/{id}` | 删除告警规则 |

## 11. 告警规则 API

告警规则用于配置设备上报数据的异常判断条件。规则可以绑定单个设备，也可以不绑定设备作为全局规则。

### 11.1 查询告警规则

```http
GET /api/alert-rules?deviceId=2&enabled=true
```

### 11.2 新增告警规则

```http
POST /api/alert-rules
```

请求示例：

```json
{
  "name": "温度过高告警",
  "deviceId": 2,
  "metric": "temperature",
  "operator": "GT",
  "thresholdValue": 80,
  "level": "ERROR",
  "enabled": true
}
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| deviceId | 可选，不传表示适用于全部设备 |
| metric | 指标：temperature、humidity、voltage、signalStrength |
| operator | 比较符：GT、LT、GTE、LTE、EQ |
| thresholdValue | 阈值 |
| level | 告警等级：INFO、WARNING、ERROR |
| enabled | 是否启用 |

## 12. 后续扩展接口

第二阶段 MQTT 接入：

- MQTT 订阅主题：`aiot/device/+/report`
- 默认 Broker：`tcp://127.0.0.1:1883`
- 消息格式与 `POST /api/device-reports` 请求体一致。
- 后端收到 MQTT 消息后复用设备上报服务，写入 `device_reports`，命中告警时写入 `logs`。
- 当前本地开发环境已安装 Mosquitto，并已验证 MQTT 正常上报、异常上报和异常日志自动生成。
- 当前配置为 `mqtt.enabled=true`，一键启动脚本会在检测到 Mosquitto 时启动或复用 `127.0.0.1:1883`。

MQTT 状态接口：

```http
GET /api/mqtt/status
```

返回当前 MQTT 是否启用、连接状态、Broker、订阅 topic、最近消息时间、消息处理计数和最近错误。

第三阶段可增加：

- `POST /api/ai/log-summary`：生成日志总结
- `POST /api/ai/error-analysis`：生成异常分析
- `POST /api/ai/maintenance-advice`：生成维护建议
