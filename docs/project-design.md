# 项目设计说明

更新时间：2026-07-09

本文合并项目需求、系统架构、数据设计、接口、页面、设备接入和部署说明。

本文是当前精简版。详细原始需求、数据库、API、页面、MQTT 和 Docker 文档保存在 `history/original-docs/`。

## 1. 项目目标

系统用于统一管理 AIoT 设备、运行日志、设备上报数据和告警规则，支持人工记录以及真实设备通过 HTTP/MQTT 自动上报。

## 2. 系统架构

```text
设备 --HTTP--> Spring Boot 后端 --MyBatis Plus--> MySQL
设备 --MQTT--> Mosquitto --> Spring Boot 订阅端
用户 --> Vue/Nginx --> /api --> Spring Boot 后端
```

技术栈：

- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios
- 后端：Java 17、Spring Boot 3.3.5、MyBatis Plus
- 数据库：MySQL 8.4
- MQTT：Mosquitto 2、Eclipse Paho
- 部署：Docker Compose、Nginx

## 3. 功能设计

- 首页：设备数量、状态统计、最近异常和维护记录
- 设备管理：新增、编辑、删除、筛选和详情
- 日志管理：新增、编辑、删除、处理状态和多条件筛选
- 标签管理：标签及日志标签关联
- 设备上报：HTTP、MQTT、数据存储和最近上报
- 告警：阈值规则、启停和异常日志自动生成
- 可视化：温度、湿度、电压和信号趋势
- MQTT 状态：连接状态、消息统计和最近错误

## 4. 数据库设计

核心表：

| 表 | 用途 |
| --- | --- |
| `devices` | 设备基础信息和当前状态 |
| `logs` | 运行、异常、维护和巡检日志 |
| `tags` | 标签 |
| `log_tags` | 日志与标签关联 |
| `device_reports` | 设备上报数据 |
| `alert_rules` | 告警阈值规则 |
| `users` | 用户预留，登录权限尚未实现 |

数据库结构以 `sql/schema.sql` 为准，演示数据以 `sql/init-data.sql` 为准。初始化脚本使用 UTF-8 和 `SET NAMES utf8mb4`。

## 5. 后端接口

主要接口：

```text
GET    /api/dashboard/summary
GET    /api/devices
GET    /api/devices/{id}
POST   /api/devices
PUT    /api/devices/{id}
DELETE /api/devices/{id}

GET    /api/logs
POST   /api/logs
PUT    /api/logs/{id}
PATCH  /api/logs/{id}/status
DELETE /api/logs/{id}

GET    /api/tags
POST   /api/tags
DELETE /api/tags/{id}

POST   /api/device-reports
GET    /api/device-reports

GET    /api/alert-rules
POST   /api/alert-rules
PUT    /api/alert-rules/{id}
DELETE /api/alert-rules/{id}

GET    /api/mqtt/status
GET    /api/enums
```

接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 6. 前端页面

```text
/dashboard       首页统计
/devices         设备管理
/devices/:id     设备详情、上报数据和趋势
/logs            日志管理
/tags            标签管理
/alert-rules     告警规则
/mqtt            MQTT 状态
```

左侧导航固定，右侧内容区域独立滚动。日志筛选修改后自动查询。

## 7. HTTP 设备接入

设备向以下地址发送 JSON：

```text
POST http://<服务器IP>:8080/api/device-reports
```

示例：

```json
{
  "deviceCode": "TEMP-HUM-001",
  "status": "NORMAL",
  "temperature": 28,
  "humidity": 55,
  "voltage": 222,
  "signalStrength": -70,
  "message": "normal report",
  "reportedAt": "2026-07-09T12:00:00"
}
```

模拟脚本：`tools/simulate-device-report.ps1`。

## 8. MQTT 设备接入

连接：

```text
Broker：<服务器IP>
Port：1883
Topic：aiot/device/{deviceCode}/report
QoS：1
```

Payload 与 HTTP 上报 JSON 相同。模拟脚本：`tools/simulate-mqtt-report.ps1`。

当前 Mosquitto 允许匿名访问，仅适用于可信局域网。公网部署必须增加账号、ACL 和 TLS。

## 9. 启动与部署

本地开发：

```text
start-all.cmd
stop-all.cmd
http://127.0.0.1:5173/
```

Docker：

```text
docker-start.cmd   日常启动
docker-update.cmd  首次部署或代码更新后重建
docker-stop.cmd    停止并保留数据
http://127.0.0.1/
```

Docker 包含前端/Nginx、Spring Boot、MySQL 和 Mosquitto。Docker 不负责设备连接 WiFi；设备仍需配置部署电脑或服务器的可访问 IP。

本地版和 Docker 版会争用 `3306`、`8080`、`1883`，不可同时运行。

## 10. 验收标准

- 设备、日志、标签和告警规则可以正常管理
- HTTP/MQTT 上报可以入库
- 异常上报自动生成设备日志
- 设备详情显示最近数据与趋势
- Docker 四服务可以启动并保持数据
- 真实设备接入后需要补充持续上报和断线重连验收

## 11. 端到端数据流程

### 人工日志

```text
用户填写表单
→ Vue 调用 /api/logs
→ Spring Boot Controller
→ LogService
→ MyBatis Plus
→ MySQL logs/log_tags
→ 前端重新查询并展示
```

### HTTP 设备上报

```text
设备连接网络
→ POST /api/device-reports
→ 参数校验
→ 根据 deviceCode 查找设备
→ 写入 device_reports
→ 更新设备状态和最后在线时间
→ 告警规则判断
→ 异常时写入 DEVICE 来源日志
→ 设备详情和趋势页面查询展示
```

### MQTT 设备上报

```text
设备连接 Mosquitto
→ 发布 aiot/device/{deviceCode}/report
→ Spring Boot Paho 订阅端收到消息
→ JSON 解析和参数校验
→ 复用 HTTP 上报的 DeviceReportService
→ 后续入库、告警和展示流程相同
```

关键理解：HTTP 和 MQTT 的入口不同，但进入后端后复用同一套业务逻辑，避免两套上报产生不同处理结果。

## 12. 模块对应关系

| 业务 | 数据表 | 后端入口 | 前端页面 |
| --- | --- | --- | --- |
| 首页统计 | `devices`、`logs` | `/api/dashboard/summary` | `/dashboard` |
| 设备管理 | `devices` | `/api/devices` | `/devices` |
| 日志管理 | `logs`、`log_tags` | `/api/logs` | `/logs` |
| 标签管理 | `tags`、`log_tags` | `/api/tags` | `/tags` |
| 设备上报 | `device_reports`、`devices`、`logs` | `/api/device-reports` | `/devices/:id` |
| 告警规则 | `alert_rules` | `/api/alert-rules` | `/alert-rules` |
| MQTT 状态 | 运行内存状态 | `/api/mqtt/status` | `/mqtt` |

## 13. 两种运行架构

本地开发版：

- Vite、Spring Boot、MySQL、Mosquitto 分别运行。
- 修改前端后可快速热更新，适合开发。
- 需要本机安装和配置各项工具。

Docker 版：

- Nginx、Spring Boot、MySQL、Mosquitto 分别运行在容器中。
- 环境统一，适合部署、演示和交付。
- 修改代码后需要执行 `docker-update.cmd` 重建镜像。

两套环境的数据相互独立，且端口重叠，因此不能同时运行。
