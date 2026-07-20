# 项目设计说明

更新时间：2026-07-13

本文合并项目需求、系统架构、数据设计、接口、页面、设备接入和部署说明。

本文是当前精简版。详细原始需求、数据库、API、页面、MQTT 和 Docker 文档保存在 `history/original-docs/`。

## 1. 项目目标

本地 AI 状态事件映射：`Local AI server discovered` 与 `local_ai_server_discovered` 显示为“已发现本地 AI 服务”，仅表示发现成功；`Local AI server connected` 与 `local_ai_connected` 显示为“已连接本地 AI 服务”；`Local AI server connection failed` 与 `local_ai_connection_failed` 显示为“本地 AI 服务连接失败”。IoT 端不得将发现事件篡改为连接成功。2026-07-12 曾在本机服务可用时验证 WebSocket 实际对话；本地服务不可用时固件应回退官方服务。`local_mqtt_broker_discovered` 则单独显示为“已发现本地日志 MQTT 服务”。固件应确保一次启动流程只创建一个本地日志客户端并只发布一轮启动事件；后端会保留收到的每条消息并在时间窗口内汇总，不应静默丢弃可能有效的设备事件。

运行日志展示约定：已知小智事件使用短中文标题，例如 `startup` → “设备启动”、`wifi_connected` → “Wi-Fi 已连接”、`mqtt_connected` → “日志 MQTT 已连接”、`official_protocol_connected` → “官方 AI 协议已连接”。后端在新记录入库时转换；前端同时兼容转换历史英文标题和消息。标题列固定单行，超出列宽时省略并通过悬浮提示保留完整文本。

日志 MQTT 连接状态映射：`Log MQTT connection failed and was retried` → “日志 MQTT 连接失败，正在重试”；`Log MQTT connection recovered` → “日志 MQTT 连接已恢复”。对应事件类型 `mqtt_connect_failed`、`mqtt_connection_failed`、`mqtt_reconnected`、`mqtt_connection_recovered` 也会映射为中文标题。

本地 AI WebSocket 状态映射：`Local AI WebSocket hello completed` 与 `local_ai_websocket_hello_completed` 显示为“本地 AI WebSocket 握手完成”，表示设备已完成本地 AI WebSocket 的 hello 握手。

运行日志汇总约定：设备端无需改变一条事件一条 MQTT 消息的发送方式。后端针对同一设备，在默认 30 秒窗口内将连续 `/log` 消息更新为同一条 `DEVICE` 日志，标题为“设备运行上报（n 条）”。每行仅保存事件标题与消息中较有信息量的一项，例如“Wi-Fi 已连接”只保留一次；前端详情将多行显示为带箭头的事件流程。`startup` 与 `firmware_started` 是固件启动批次边界，必须强制创建新日志，避免设备复位发生在窗口内时与前一轮事件混合。窗口由 `mqtt.runtime-log-merge-window-seconds`（环境变量 `MQTT_RUNTIME_LOG_MERGE_WINDOW_SECONDS`）配置；其余事件超过窗口会新建日志。

日志列表实时性：日志管理页在页面可见时每 5 秒重新请求当前筛选条件和当前分页。标签页隐藏、路由离开时停止定时器；返回标签页时立即刷新。自动刷新不改变筛选、分页、编辑框或详情框状态。

日志批量删除：默认隐藏勾选列，用户点击筛选栏“重置”右侧的“批量删除”后才显示行勾选和当前页全选，并通过 `DELETE /api/logs`、请求体 `{ "ids": [1, 2] }` 批量删除。后端先确认所有日志均存在，再在同一事务中删除 `logs` 记录及对应 `log_tags` 关联；前端必须经二次确认，完成或取消均清除表格跨页保留的选择，且不提供无条件“清空全部”操作。

网络切换验证：2026-07-12 已验证电脑和小智同时连接手机热点时，固件通过 UDP `19830` 自动发现热点下电脑的新局域网 IPv4，并使用 Mosquitto TCP `1883` 成功上传日志。设备无需重新编译或烧录；热点必须允许客户端互访，Windows 对该网络应使用专用网络防火墙规则。

断网恢复验证：2026-07-12 已完成实机验证。设备网络临时断开后，固件后台重试日志 MQTT 连接；网络恢复时重新发现或连接局域网 Broker，并恢复 `/log` 运行事件上传。该恢复过程不改变官方 AI 服务地址，也不要求重新烧录固件。

MQTT 认证设计：当前采用全局设备账号模式。MQTT 状态页注册的一套用户名/密码会保存到不纳入版本控制的 `config/mqtt-credentials.env` 与 Mosquitto 密码文件；设备和后端均使用该账号，通用 ACL 允许读写 `aiot/device/+/report` 与 `aiot/device/+/log`。设备通过配网页保存相同凭证；留空或认证失败时仅停止本地日志传输。新建认证时先关闭匿名访问并重启本地服务加载新配置，再在设备填写凭证；只有为既有匿名设备平滑迁移时，才先配置设备。该模式仍可用 Topic 和 `deviceCode` 识别设备，但无法阻止持有全局凭证的设备伪造其他设备 Topic，适合家庭/开发环境。

系统用于统一管理通用 AIoT 设备、运行日志、设备上报数据和告警规则，支持人工记录以及真实设备通过 HTTP/MQTT 自动上报。小智 ESP32-S3 是当前实机验证的接入示例；系统主体仍以通用 MQTT、HTTP 和协议适配器方式支持不同设备类型。

## 2. 系统架构

可维护的 Mermaid 架构图源文件见 [`architecture.mmd`](architecture.mmd)，GitHub 项目入口使用同一架构表达设备的 AI 双通道、独立 MQTT 日志通道、UDP Broker 自动发现、Mosquitto、Spring Boot、MySQL 与 Vue 管理页面之间的关系。

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

容器就绪检查使用 Spring Boot Actuator 的 `GET /actuator/health`。管理服务独立监听 `127.0.0.1:8081`，Docker 不映射该端口；只开放总体健康状态且不显示组件详情。该端点不使用业务接口的统一响应包装，也不返回数据库、MQTT、主机或凭证信息。

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

当前 Mosquitto 已关闭匿名访问并启用全局 MQTT 凭证，仍仅适用于可信局域网。公网部署必须启用 TLS，并升级为每设备账号和 ACL。

公开仓库仅提供 `config/mosquitto-lan.example.conf`、`config/mosquitto-acl.example.conf`、`config/mqtt-credentials.env.example` 与 `backend/src/main/resources/application.example.yml`。实际凭证、本地 Mosquitto 配置和本机后端配置必须由部署者在本地创建，且不得纳入版本控制。

本地 ESP32 联调使用 `config/mosquitto-lan.conf`，由 `start-all.ps1` 启动本机 Mosquitto 并监听 TCP `1883`。Windows 已建立仅限“专用网络”和“本地子网”的入站防火墙规则；不配置路由器端口转发，也不向公网开放该端口。固件通过局域网发现获取当前可访问的私有 IPv4，因此切换局域网不需要重新刷写固件。

本地启动脚本通过端口轮询确认 MySQL、Mosquitto、后端和前端可用。轮询间隔为 200 毫秒；后端使用 `mvn spring-boot:run` 的实际初始化通常仍需约 2 秒，脚本等待该过程是为了避免前端在后端未就绪时出现请求失败。

### 8.3 局域网 Broker 自动发现

为避免设备和运行 IOT 服务的电脑切换 WiFi、网线或手机热点后因 DHCP 地址变化而重新刷写固件，设备应在同一局域网内先发现 MQTT Broker，再建立独立日志连接。该功能由两部分组成：ESP32 固件发送发现请求并校验响应；IOT 侧的 `MqttDiscoveryResponder` 在 UDP `19830` 返回当前可访问的 Broker 私有 IPv4、端口和 TLS 信息。发现超时或失败时，固件使用 menuconfig 中的手动 Broker 地址作为联调兜底。

发现请求与响应使用 `protocol: aiot-mqtt-discovery-v1`，请求必须包含非空 `nonce`，响应将其原样回传，并向请求来源地址和端口单播。服务端仅响应同网段请求并返回 RFC1918 私有 IPv4。配置键为 `mqtt.discovery.enabled`、`port`、`broker-host`、`broker-port`、`tls` 和 `token`；当前 token 为空时允许匿名发现，配置 token 后必须精确匹配。Windows 需要放行专用网络本地子网的 UDP `19830`，测试脚本为 `tools/test-mqtt-discovery.ps1`。

发现功能只适用于设备和电脑可互访的同一局域网；手机热点若启用客户端隔离或阻止 UDP 广播，自动发现可能失败，应回退手动地址。非演示环境应配置共享 token，并进一步改用每设备凭证和 TLS，避免局域网内被伪造 Broker 诱导。

2026-07-11 首次真实硬件联调已通过：小智 `XIAOZHI-001` 自动发现本机 Broker，并发布 `State changed` 状态事件到 `aiot/device/XIAOZHI-001/report`；后端已接收、处理并写入 `device_reports`。后续仍需验证长时间运行、网络断开重连和不同 WiFi/热点环境。

同日已验证运行日志 Topic：`startup`、`wifi_connected`、`mqtt_connected`、`official_protocol_connected` 都写入 `logs`，来源为 `DEVICE`。启动早期 ESP32 尚未同步系统时间时会产生 `1970-01-01`；固件应在时间未同步时省略 `reportedAt`，后端会自动使用接收时间。

### 8.1 小智 AI 双通道接入

小智 AI 设备当前采用本地优先、官方回退的双通道模式：本地小智服务可用时通过局域网发现、OTA 配置和 WebSocket 使用本地 AI；本地服务不可用时回退官方 AI。独立本地 MQTT 遥测客户端仍负责日志上报：

```text
小智设备 --官方 WebSocket/MQTT--> 官方服务器：AI 对话、语音识别和 TTS
小智设备 --本地 MQTT-----------> Mosquitto：心跳、状态、故障和关键事件
小智设备 --USB 串口------------> 开发电脑：详细调试日志
```

设计约束：

- 本地 MQTT 连接、重连或发送失败不得阻塞官方 AI 通道。
- 生产通道只上报有业务价值的状态与事件，不逐行上传固件调试输出。
- 详细调试输出通过 USB 串口查看。
- 固件修改前必须确认准确板型、源码版本和分区配置，并备份现有固件。
- 周期状态继续发布到 `aiot/device/{deviceCode}/report`。
- 设备运行事件发布到 `aiot/device/{deviceCode}/log`，后端订阅后写入 `logs` 表，来源为 `DEVICE`。

运行日志 Payload：

```json
{
  "deviceCode": "XIAOZHI-001",
  "eventType": "wifi_connected",
  "level": "INFO",
  "message": "Wi-Fi connected",
  "reportedAt": "2026-07-11T00:47:12"
}
```

`level` 可选值为 `INFO`、`WARNING`、`ERROR`；INFO 默认归类为已解决的 `RUNNING` 日志，WARNING/ERROR 默认归类为待处理的 `ERROR` 日志。小智应仅发布启动、网络连接/断开、官方协议错误/重连、MQTT 发现/连接失败等关键事件，不发布高频语音状态切换。

### 8.2 不同设备的接入原则

不同设备统一经过协议适配层进入日志系统：

```text
MQTT / HTTP / 串口 / 厂商 API / 私有协议
→ 协议适配器或边缘网关
→ 统一设备事件
→ Spring Boot
→ MySQL
```

优先支持通用 MQTT、通用 HTTP 和 USB/串口网关。厂商私有协议通过独立适配器转换，避免在核心日志业务中为每种设备硬编码。

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

首次运行 `docker-update.cmd`（或后续运行 `docker-start.cmd`）会调用 `tools/initialize-docker-config.ps1`，在本机被忽略的 `.env` 和 `docker/local/` 中生成或复用数据库密码、MQTT 用户名密码、Mosquitto 密码文件和 ACL。Docker Mosquitto 使用这些私有文件关闭匿名访问；后端挂载同一私有目录，通过 `mqtt-credentials.properties` 读取凭证。脚本会更新当前宿主机的局域网 IPv4，并由后端对外发布 UDP `19830` 自动发现响应；默认不配置发现 Token，确保未预置 Token 的设备可自动发现 Broker。需要额外防护时，仅在设备固件也保存同一 Token 的前提下于私有 `.env` 手动设置 `MQTT_DISCOVERY_TOKEN`。没有可用局域网 IPv4 时仍可运行管理系统，但自动发现会暂时关闭。不得提交 `.env`、`docker/local/` 或其中任何内容；此方案仅适用于可信局域网，不得直接暴露公网。

群晖 DSM 的 Container Manager 不执行 Windows 启动脚本。部署源码构建版前，必须上传完整项目目录（至少包含 `docker-compose.yml`、`backend/`、`frontend/`、`sql/`、`docker/`、`tools/`），再通过 SSH 在项目根目录运行 `sh tools/initialize-synology-docker-config.sh`。如已拉取或导入 GHCR 前后端成品镜像，应运行 Windows `tools/create-synology-image-deploy.ps1` 生成仅含 `docker-compose.ghcr.yml`、表结构 SQL 和初始化脚本的部署包，不需要上传 `backend/` 或 `frontend/` 源码。首次启动只挂载 `sql/schema.sql`，新用户数据库为空；`sql/init-data.sql` 仅作为开发演示数据保留。该 POSIX shell 脚本在 NAS 上生成同样的私有 `.env` 和 `docker/local/`，随后才可在 Container Manager 创建项目；缺少 `sql/schema.sql` 或 `docker/local/mosquitto-lan.conf` 会导致绑定挂载失败。2026-07-14 已完成群晖成品镜像实机验收，具体过程与注意事项见 `synology-nas-deployment.md`。

本地版和 Docker 版会争用 `3306`、`8080`、`1883`、UDP `19830`，不可同时运行。

成品镜像部署使用 `docker-compose.ghcr.yml` 与 `docker-ghcr-start.cmd`、`docker-ghcr-update.cmd`、`docker-ghcr-stop.cmd`。该编排仅引用 GHCR 的前端和后端镜像，MySQL、Mosquitto、命名卷及私有 `.env`/`docker/local/` 生成策略与源码构建版一致。仓库内 GitHub Actions 在 `main` 或版本标签推送后构建镜像；首次发布后必须将 GitHub Packages 可见性设为 Public，才能作为公开部署入口。源码构建版和 GHCR 成品镜像版不得同时运行。

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
