# HiveMQ Cloud 远程 MQTT 接入版本

更新时间：2026-07-15

## 1. 目的与状态

本文记录 AIoT-Log-System `Remote-Hivemq` 分支的远程 MQTT 接入版本。它用于 IoT 设备与家中群晖不在同一网络时的日志传输。

当前状态：**后端、远程 Docker/GHCR/群晖编排和小智独立日志客户端的远程模式代码已实现；后端 Maven 与前端生产构建已通过。2026-07-15 已完成 Windows Docker + MQTTX + HiveMQ Cloud + Spring Boot + MySQL 的端到端日志入库验收。真实凭证未写入仓库。**

### MQTTX 手工验证记录（2026-07-14）

- 已在 HiveMQ Cloud 中创建仅覆盖 `aiot/device/#` 的 Publish + Subscribe 权限及对应凭证。
- 已使用 MQTTX 通过 HiveMQ 提供的完整域名建立 `mqtts` TLS 连接，端口为 `8883`，并启用 CA 签发服务器证书校验。
- 已成功订阅 `aiot/device/#`（QoS 1）。
- 已向 `aiot/device/TEST-001/log`（QoS 1）发布 JSON 测试日志，并在 MQTTX 的“已接收”列表收到同一消息。

该验证确认了 DNS、TLS、HiveMQ 账号凭证与 `aiot/device/#` 发布/订阅权限可用。本文不记录 Broker 域名、用户名、密码或 Token；该结果也不代表 Spring Boot、群晖 Docker、数据库入库、ESP32 固件或局域网模式已经完成验证。

## 2. 与现有局域网版本的关系

远程版本是与现有局域网版本并列的可选版本，**不替换、不删除、不改变**当前默认局域网流程。

```text
局域网版本（默认，保持不变）
设备 → UDP 19830 自动发现 → 本地 Mosquitto:1883 → Spring Boot → MySQL

远程版本（新增，可选）
异地设备 → HiveMQ Cloud TLS:8883 → 群晖 Spring Boot 主动订阅 → MySQL
```

两个版本复用相同的 MQTT Topic、JSON Payload、日志入库逻辑和 Vue 管理页面：

```text
aiot/device/{deviceCode}/report
aiot/device/{deviceCode}/log
```

局域网版本继续使用 UDP `19830`；远程版本不使用该发现协议，因为 UDP 广播不能跨互联网，也不会开放家庭网络端口。

## 3. 远程版本目标

- Broker 使用 HiveMQ Cloud Serverless FREE。
- 设备使用 HiveMQ 提供的域名、MQTT TLS 端口 `8883` 和用户名密码。
- 群晖 Docker 中的 Spring Boot 后端主动通过 TLS 订阅 `aiot/device/+/report` 与 `aiot/device/+/log`。
- 群晖不需要公网 IP；不需要配置路由器端口转发；不将家庭 `1883`、`8080`、`3306` 或 UDP `19830` 暴露到公网。
- 当前阶段所有本人设备共用一套 MQTT 凭证，通过 Topic 中的 `deviceCode` 区分设备。
- HiveMQ 权限限制为 `aiot/device/#` 的 Publish + Subscribe。

共享凭证只能用于受控的个人设备环境：持有该凭证的客户端可能伪造其他 `deviceCode` 的 Topic。每设备独立账号、最小 ACL 与轮换机制属于后续安全增强项。

## 4. 已实现的模式切换

后端使用显式 `MQTT_MODE`；默认值仍为 `lan`：

| 模式 | Broker 来源 | 传输 | UDP 19830 | 用途 |
| --- | --- | --- | --- | --- |
| `lan` | 本地 Mosquitto / 自动发现 | TCP `1883` | 启用 | 设备与群晖或开发电脑同网段 |
| `remote` | 用户配置的 HiveMQ 域名 | TLS `8883` | 关闭 | 设备与群晖位于不同网络 |

远程 Docker/GHCR 模式由被 Git 忽略的 `docker/local/hivemq-remote.env` 提供私有配置；它与局域网 Docker 的 `.env` 分离。公开仓库仅保留 `config/hivemq-remote.env.example` 占位模板：

```text
MQTT_MODE=remote
MQTT_BROKER_URL=ssl://<private-host>:8883
MQTT_USERNAME=<private-username>
MQTT_PASSWORD=<private-password>
```

不得把真实 HiveMQ 域名、用户名、密码、Token、CA 私钥、设备 MAC、局域网 IP、运行日志或数据库数据提交到 Git。

## 5. 实现内容

### 后端

- 后端使用 Eclipse Paho 1.2.5 的 `ssl://` URL；远程模式强制校验 TLS URL 与用户名密码。
- 订阅逻辑继续同时订阅 `aiot/device/+/report` 与 `aiot/device/+/log`，复用原有入库业务。
- 远程模式下 UDP `19830` 响应器强制关闭，管理页不会显示真实 Broker 地址或允许修改本地 Mosquitto 凭证。
- 新增 `docker-compose.remote.yml` 与 `docker-compose.remote.ghcr.yml`：只运行 MySQL、后端、前端，不启动 Mosquitto、不映射 `1883`、不映射 UDP `19830`。
- 两个 GHCR 包以标签区分版本：`v1.0.0-lan` 是固定局域网版，`v1.1.0-remote-mqtt` 是固定远程版。远程群晖 Compose 固定拉取远程标签，避免误用 `latest`；`latest` 只允许 `main` 分支推送更新并保持局域网语义，版本标签事件不得覆盖它。版本标签构建完成后，工作流会创建同名 GitHub Release，显示在仓库 Releases 区域。
- 新增 Windows `docker-remote-*.cmd`、远程私有 `.env` 初始化脚本，以及 `tools/create-synology-image-deploy.ps1 -Remote` 的群晖成品镜像部署包支持。

### 小智固件

- 独立日志客户端使用 `MQTT_TRANSPORT_OVER_SSL`、ESP X.509 CA bundle，并为远程域名设置 `common_name` 以确保 TLS 主机名校验与 SNI。
- `aiot_log` 独立 NVS 配置已增加持久化的 `remote_mode` 与 `tls`；远程模式仅接受主机名、TLS 和端口 `8883`。
- 配网页可选择“远程 HiveMQ 模式”；该模式跳过 UDP 自动发现，仍只影响独立日志 MQTT 客户端。
- 修改仅限独立 AIoT 日志 MQTT 客户端；不得修改小智官方 AI、OTA、WebSocket 或本地 AI 回退通道。

## 6. TLS、CA 与 SNI 约束

- 必须配置 HiveMQ 提供的**域名**，不得使用解析后的 IP；域名是证书校验与 SNI 的前提。
- 后端将依赖 Java 17 默认信任库验证 HiveMQ 的公开 CA；只有实际证书链不兼容时，才增加私有 truststore/CA 的可选配置，不能关闭验证。
- 固件将继续使用 ESP-IDF CA bundle，保持证书主机名验证，不允许使用跳过证书校验的实现。
- 最终以真机连接 HiveMQ 域名的结果确认 CA bundle、DNS、TLS 版本及 SNI 兼容性。

## 7. 使用方式与验收

1. 在 HiveMQ Cloud 创建 Publish + Subscribe 且 Topic Filter 为 `aiot/device/#` 的权限和凭证。
2. 使用 MQTTX 测试 HiveMQ 域名、TLS `8883`、账号密码和 Topic 权限。
3. 在 `Remote-Hivemq` 分支运行 `docker-remote-update.cmd`（源码）或 `docker-remote-ghcr-update.cmd`（成品镜像）。脚本先生成私有 `docker/local/hivemq-remote.env`；只在其中填写占位值对应的 HiveMQ 信息。
4. 使用 MQTTX 向 `/report` 与 `/log` 发送有效数据，确认后端订阅、数据库入库和管理页展示。
5. 烧录小智远程版本，在配网页选择远程模式，填写 HiveMQ 域名、端口 `8883` 和共享凭证；在不同 Wi-Fi 或手机热点下验证远程日志上报。
6. 将后端设为 `MQTT_MODE=lan` 或使用原有局域网编排，回归验证 UDP `19830` 自动发现、本地 Mosquitto、断网恢复和既有日志链路。
7. 验证群晖远程模式只有到 HiveMQ `8883` 的出站 TLS 连接，且没有新增公网入站端口。

## 8. 2026-07-15 Windows Docker 端到端验收与故障处理

### 已完成的端到端验证

- 远程 Compose 已在 Windows Docker Desktop 中完成构建并启动：MySQL 健康检查通过，Spring Boot 后端和 Vue/Nginx 前端均正常启动；远程编排未启动 Mosquitto，也没有使用 UDP `19830`。
- 后端 MQTT 状态页确认 `remote` 模式、TLS 连接成功、订阅 `aiot/device/+/report` 与 `aiot/device/+/log` 后可接收 HiveMQ 消息。
- MQTTX 向 `aiot/device/TEST-001/log`（QoS 1）发送测试 JSON 后，后端已成功处理并写入 MySQL；至此完成 MQTTX → HiveMQ Cloud → Spring Boot → MySQL 的远程日志链路验证。

### 测试数据校验结论

- Topic 中的设备编号、Payload 的 `deviceCode` 和管理端已创建的设备编号必须完全一致；不一致时后端会拒绝消息并显示设备不存在。
- `logType` 仅允许 `RUNNING`、`ERROR`、`MAINTENANCE`、`INSPECTION`。测试中 `SYSTEM` 被后端拒绝为“日志类型不合法”；改为 `RUNNING` 后处理成功。
- 上述拒绝说明 TLS、订阅和消息到达正常，属于入库数据校验，而非 HiveMQ 连接故障。

### Windows 主机环境问题与恢复

- 问题：Windows 异常重启后，Docker Desktop 可能停留在 “Starting the Docker Engine”，WSL 报 `C:\Program Files\WSL\system.vhd` 无法附加、`HCS/ERROR_NOT_SUPPORTED`。
- 恢复：以管理员身份执行 `bcdedit /set hypervisorlaunchtype auto`，重启 Windows；随后用 `wsl -d Ubuntu-22.04 -- echo WSL_OK` 确认 WSL 2 已恢复，再启动 Docker Desktop。不要删除或注销 WSL 发行版、`system.vhd`、Docker 数据卷或私有远程配置。
- 问题：Docker Hub 的匿名 Token 请求偶发连接超时，导致构建镜像元数据拉取失败。
- 处理：先预拉取 `maven:3.9.9-eclipse-temurin-17`、`node:22-alpine`、`eclipse-temurin:17-jre`、`nginx:1.27-alpine`；本机使用 localhost 系统代理时，在 `%UserProfile%\.wslconfig` 配置镜像网络与 `autoProxy=true`，然后执行 `wsl --shutdown` 并重启 Docker Desktop。该配置只处理 Windows/WSL 网络，不包含 HiveMQ 凭证。
- 另有 Windows 截图工具在选区后卡死的现象；它与项目容器和 HiveMQ 链路无关，应按 Windows 应用/显卡驱动问题单独处理，项目验证可先使用终端输出而非截图。

## 9. 2026-07-15 群晖远程版实机验收

- 使用 `tools/create-synology-image-deploy.ps1 -Remote` 生成无源码部署包，上传到独立 NAS 目录后，由 `initialize-synology-docker-config.sh` 创建权限为 `0600` 的私有 HiveMQ 配置；真实域名和凭证未进入仓库或文档。
- 群晖 Docker CLI 不提供 `docker compose` V2 子命令，因此使用 DSM Container Manager 根据部署包中的 `docker-compose.yml` 创建远程项目；前后端固定拉取 `v1.1.0-remote-mqtt`。
- 既有 NAS 服务占用了宿主机 `8080`，导致后端首次启动时报 external connectivity；将后端宿主机端口调整为 `18080`、前端调整为 `18000` 后重新创建，容器内部仍通过 `backend:8080` 通信。
- MySQL 健康检查、后端和前端启动成功；管理端 MQTT 状态确认 HiveMQ TLS 已连接。创建匹配的测试设备后，MQTTX 发布的有效日志被后端收到并处理成功，完成 HiveMQ Cloud → 群晖 Spring Boot → MySQL 的实机验收。
- 原局域网项目和数据未删除；由于两套编排默认端口重叠，验收时不同时启动。未开放家庭 MQTT、数据库或后端端口到公网。

## 10. 实施涉及的主要文件

- AIoT 后端 MQTT 配置、订阅客户端、示例配置、状态页和 Docker/GHCR 编排。
- Windows Docker 私有配置初始化脚本、群晖初始化脚本和成品镜像包生成器。
- 小智固件的 `mqtt_log_client`、AIoT NVS 配置与配网页面；不涉及官方 AI、OTA 或 WebSocket 通道。
- 项目上下文、状态、部署文档和本文件。
