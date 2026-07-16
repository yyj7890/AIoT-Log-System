# HiveMQ Cloud 远程 MQTT 接入版本

更新时间：2026-07-16

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
- 两个 GHCR 包以标签区分版本：`v1.0.0-lan` 是固定局域网版，`v1.1.0-remote-mqtt` 是首个远程版，`v1.1.1-remote-mqtt` 增加远程状态中文文案和 QoS 1 相邻重投保护，`v1.1.2-remote-mqtt` 增加 MQTT 状态页自动刷新，当前 `v1.1.3-remote-mqtt` 增加 `WARN` 等级兼容并将状态页和日志管理页统一为 1 秒可靠刷新。远程群晖 Compose 固定拉取当前远程标签，旧远程标签保留用于回退，避免误用 `latest`；`latest` 只允许 `main` 分支推送更新并保持局域网语义，版本标签事件不得覆盖它。版本标签构建完成后，工作流会创建同名 GitHub Release，显示在仓库 Releases 区域。
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

## 11. 2026-07-16 小智真机上报与假异常修复

- 小智远程固件已成功通过 HiveMQ TLS 上传启动运行日志，确认 DNS、TLS `8883`、账号权限、Topic、Spring Boot 订阅和入库链路可用；未记录真实域名或凭证。
- 首次汇总出现“日志 MQTT 连接失败，正在重试 → 已恢复 → 已连接”的不合理序列。根因在固件状态机：等待可信系统时间被当成失败，主动停止客户端产生的断开事件也可能触发失败标志，恢复分支又未设置首次连接标志。
- 固件 `v2.2.6-aiot-remote-mqtt.2` 已把连接尝试拆分为 `Connected`、`Deferred`、`Failed`；可信时间等待不再产生异常，主动停止的断开事件被忽略，恢复后不再重复上报“已连接”，真实 Broker 断开即使队列为空也会后台重连。
- 真实失败事件统一使用 `mqtt_connection_failed`，与后端和前端既有中文映射一致。
- 后端汇总保留最高严重级别作为故障历史，但明确收到 `mqtt_reconnected` 或 `mqtt_connection_recovered` 后将状态改为 `RESOLVED`；后续再次收到非 `INFO` 故障会重新置为 `PENDING`。
- JDK 17 Maven 测试/编译通过；固件对象编译、完整主组件归档、ELF 链接、BIN 生成和分区检查通过。修复版尚待重新烧录、冷启动、断网恢复、异地网络和局域网回归验证。

## 12. 2026-07-16 远程状态文案与 QoS 1 相邻重投幂等

### 中文显示

- `Remote log MQTT TLS connected` 映射为“远程日志 MQTT（TLS 8883）已连接”。
- `Remote log MQTT TLS connection failed and was retried` 映射为“远程日志 MQTT（TLS 8883）连接失败，正在重试”。
- `Remote log MQTT TLS connection recovered` 映射为“远程日志 MQTT（TLS 8883）连接已恢复”。
- 后端负责新入库运行事件的中文化，前端同时兼容历史英文单行和多行汇总内容。

### 去重边界

- MQTT 继续使用 QoS 1，不通过降低 QoS 规避重复。
- 仅当事件位于同一启动批次、同一 30 秒汇总窗口、紧邻上一事件，且标准化后的摘要完全相同时，后端不再重复追加、增加计数或改变状态。
- 不相邻的相同事件不会被全局删除；内容不同的事件正常追加；`startup` 和 `firmware_started` 始终开始新的启动批次。
- 不单独依赖 MQTT DUP 标志，因为首次业务处理失败后的合法重投仍需要被处理。

### 兼容性与状态

- 保留 `mqtt_connected`、`mqtt_connection_failed`、`mqtt_reconnected` 事件类型，未改变固件与后端协议。
- 保留真实故障为 `PENDING`、恢复为 `RESOLVED`、后续新故障重新为 `PENDING` 的状态流转。
- 汇总仍保留历史最高严重级别；明确恢复事件只关闭当前故障状态，不抹除故障历史。

### 验证结果与待办

- JDK 17 下后端 7 项测试全部通过，覆盖三种远程中文映射、相邻完全相同事件去重、不同事件追加、启动批次边界和故障/恢复/再故障状态流转。
- 前端 TypeScript 检查和 Vite 生产构建通过。
- 本轮不读取真实 HiveMQ 配置；版本镜像发布与群晖部署分开执行，发布镜像不会自动修改群晖项目。
- 后续需用新远程前后端镜像更新群晖项目，再执行冷启动页面、QoS 1 相邻重投、真实断网恢复、局域网模式和单后端实例检查。

### `v1.1.1-remote-mqtt` 发布

- 本轮修复以独立标签 `v1.1.1-remote-mqtt` 发布前后端镜像，不覆盖或删除 `v1.1.0-remote-mqtt`。
- `docker-compose.remote.ghcr.yml` 固定引用 `v1.1.1-remote-mqtt`；群晖更新时只需拉取两个新镜像并重新构建现有远程项目，继续复用原数据库卷和私有 `docker/local/hivemq-remote.env`。
- 发布镜像不代表群晖已经更新；群晖部署和真机复验仍需单独执行。

## 13. 2026-07-16 MQTT 状态页自动刷新

- MQTT 状态页在浏览器页面可见时每 1 秒自动获取一次连接状态、收到/成功/失败计数和最近运行信息，不再要求用户点击“刷新”才能看到新计数。
- 浏览器标签页隐藏时暂停轮询，返回该标签页时立即刷新，减少无效请求。
- 自动刷新和手动刷新共用请求互斥，避免同一时刻重复请求；手动“刷新”按钮继续保留。
- 远程模式的 Broker 仍显示脱敏占位符，HiveMQ 域名、用户名和密码继续只保存在部署环境私有文件中；连接模式、Client ID、Topic 和 QoS 属于运行配置，只读展示。
- 该改动仅影响前端状态展示，不改变 MQTT 订阅、QoS 1、日志入库或设备上报行为；需要发布新前端镜像并更新群晖项目后才会生效。

### `v1.1.2-remote-mqtt` 发布

- MQTT 状态页自动刷新以独立标签 `v1.1.2-remote-mqtt` 发布前后端镜像，不覆盖或删除 `v1.1.1-remote-mqtt`、`v1.1.0-remote-mqtt` 或局域网 `v1.0.0-lan`。
- `docker-compose.remote.ghcr.yml` 固定引用 `v1.1.2-remote-mqtt`；群晖更新时继续复用现有数据库卷和私有 `docker/local/hivemq-remote.env`。
- 发布完成后需要在群晖重新拉取两个镜像并重建远程项目，才能在页面看到自动刷新效果。

## 14. 2026-07-16 日志等级兼容与实时列表刷新

### “日志等级不合法”的根因

- HiveMQ TLS、Topic 和后端订阅均正常；错误发生在消息到达 IoT 后端后的业务校验阶段。
- 小智日志客户端接受并上报 `INFO`、`WARN`、`ERROR`，而 IoT 日志枚举使用 `INFO`、`WARNING`、`ERROR`。因此 `WARN` 消息会增加“收到消息”和“处理失败”，但不会入库。
- 后端设备运行日志入口现在会去除首尾空格、忽略大小写，并将 `WARN` 规范化为系统标准值 `WARNING` 后再校验和入库。该兼容仅作用于设备运行日志入口，不放宽管理端人工日志的枚举约束。
- 现有小智固件无需因该枚举差异重新烧录；后续固件仍可继续发送 `WARN`。MQTT 状态页的历史失败计数属于当前后端进程内累计值，部署修复后重启后端会重新计数。

### MQTT 状态页与日志管理页

- 两个页面共用 `1000 ms` 刷新间隔；页面不可见时暂停，返回时立即刷新。
- 自动请求使用互斥标志，避免上一次请求尚未完成时继续堆积；日志列表自动刷新不显示加载遮罩，手工筛选、分页和编辑后的刷新仍保留正常加载反馈。
- 状态和日志查询都附加无缓存参数及请求头，Nginx 对 `/api/` 响应增加 `Cache-Control: no-store`，避免浏览器或中间层返回旧数据。
- 日志列表按 `updatedAt`、`id` 倒序排列，并显示“更新时间”。这样同一条“设备运行上报（n 条）”在 30 秒汇总窗口内追加内容时，会重新出现在列表顶部，而不是因为 `createdAt` 不变看起来没有刷新。
- 选择 1 秒而不是更短间隔，以控制群晖 Spring Boot 与 MySQL 的持续查询负载；如需更实时的推送展示，后续应改用 SSE 或 WebSocket，而不是继续缩短轮询时间。

### 验证

- JDK 17 Maven 测试通过：8 项测试、0 失败，其中新增 `WARN` 规范化为 `WARNING` 的用例。
- 前端 TypeScript 检查和 Vite 生产构建通过。
- 验证过程未读取真实 HiveMQ 配置；发布只推送源码提交和版本标签，不会自动修改群晖项目或私有环境文件。

### `v1.1.3-remote-mqtt` 发布

- 本轮修复以独立标签 `v1.1.3-remote-mqtt` 发布前后端镜像，不覆盖或删除 `v1.1.2-remote-mqtt`、更早远程标签或局域网 `v1.0.0-lan`。
- `docker-compose.remote.ghcr.yml` 固定引用 `v1.1.3-remote-mqtt`；群晖更新时继续复用现有数据库卷和私有 `docker/local/hivemq-remote.env`。
- 小智固件无需为 `WARN` 兼容重新烧录；群晖必须更新后端镜像后，新的警告日志才会被规范化并成功入库。
