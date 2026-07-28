# HiveMQ Cloud 远程 MQTT 接入版本

更新时间：2026-07-27

## 1. 目的与状态

### 2026-07-27：主动播报后端本地源码阶段

远程后端新增固定测试 Opus 的 MQTT 发布和 ACK 接收源码：使用现有 TLS Paho 客户端发布 `announcement/command` 与 `announcement/audio/{taskId}/{frameIndex}`，订阅 `announcement/ack`，并通过 Flyway V3 保存任务和 `received`、`played`、`failed` 时间线。发布固定为 QoS 1、`retain=false`，音频为 16 kHz 单声道 60 ms 裸 Opus packet。现有 `/report`、`/log` 保持不变。

本阶段使用固件仓库已有 `zh-CN/welcome.ogg` 在本地拆分出的 35 个裸 Opus packet 作为固定测试资源，不调用或部署 TTS。设备启动后，独立本机 MQTT Client ID 已通过真实 HiveMQ 完成一次受控任务：后端发布及持久化、设备 `received` ACK、自动播放和 `played` ACK 均已验证。固定测试入口仍默认关闭；后续再测试重复、过期、忙碌和与官方 AI 对话并发的边界。没有部署或群晖改动，也没有烧录设备。

动态 TTS 第三阶段源码：IoT 不在 Java 容器内安装 Piper、ffmpeg 或编码器；未来私有本地网关负责文本到 16 kHz 单声道 60 ms 裸 Opus packet。启用 `ANNOUNCEMENT_TTS_ENABLED` 后，IoT 向私有 `ANNOUNCEMENT_TTS_BASE_URL` 的 `POST /v1/announcements/opus` 请求文本与音频参数，再验证帧并复用现有 MQTT 二进制发布和 ACK。HTTP 响应可仅在内网以 Base64 承载帧；MQTT 始终发布原始二进制、绝不使用 Ogg 或 Base64。默认关闭，未部署任何 TTS 服务。

本机已构建 `local/xiaozhi-local-tts:0.1.0` 和离线包 `dist/xiaozhi-local-tts-0.1.0.tar`。群晖独立项目模板在 `components/local-tts-gateway/compose.synology.yml`：仅绑定本机端口、只读模型卷、临时目录 64 MB、内存上限 512 MB。模型不包含在镜像中；无模型请求返回安全 503。尚未导入或部署群晖。

桥接器已将私有 `DEFAULT_DEVICE_CODE` 传给提醒 MCP 子进程，并以它优先于官方小智调用传入的 `device_code`。官方小智创建的一分钟提醒已实机投递到目标小智并主动播放。提醒状态 `PUBLISHED` 仍只表示后端已发布，验收仍以设备 `received`/`played` ACK 与实际播放为准；旧任务的 MQTT 消息为 `retain=false`，不会自动投递到新设备。当前播报固定测试 Opus，动态 TTS 尚未实现。

本文记录 AIoT-Log-System `Remote-Hivemq` 分支的远程 MQTT 接入版本。它用于 IoT 设备与家中群晖不在同一网络时的日志传输。

当前状态：**`v1.2.3-remote-mqtt` 已发布前后端 GHCR、来源证明及 GitHub Release；下行 manifest 直接用 UTC `Instant` 生成，消除容器默认时区造成的过期时间偏差。群晖已导入并完成固定 Opus 主动播放与 ACK 实机验收。真实凭证未写入仓库。**

真实联调补充：`v1.2.2` 的 `LocalDateTime` 到 UTC 转换仍依赖运行容器的 JVM 默认时区；在当前部署中造成 wire `expiresAt` 比设备 UTC 落后约八小时，固件正确返回 `failed/expired`。下一修复版直接使用 `Instant.now()` 与 `Instant.plus(5 minutes)` 生成 manifest 的 `createdAt`、`expiresAt`，不改变数据库展示时间、MQTT Topic 或音频格式。

### 统一版本变更总表

本表是固定镜像版本差异的统一入口。历史章节继续保存问题原因和验收过程，但判断“某个镜像版本修改了什么”时以本表为准。前端和后端使用同一个固定标签成对发布；某一端没有功能变化时仍可能同步重建，以保持部署标签一致。

| 固定标签 | 状态 | 前端/页面变化 | 后端/数据库变化 | 部署与兼容说明 |
| --- | --- | --- | --- | --- |
| `v1.0.0-lan` | 已发布 | 局域网管理页面基线 | 本地 Mosquitto、HTTP/MQTT 上报及日志管理基线 | 固定局域网版；`latest` 继续保持局域网语义 |
| `v1.1.0-remote-mqtt` | 已发布、曾完成群晖验收 | 增加远程 MQTT 模式状态展示 | 增加 HiveMQ TLS `8883` 订阅、远程配置校验，关闭远程模式 UDP 发现 | 首个远程版；新增远程源码/GHCR Compose和群晖部署包 |
| `v1.1.1-remote-mqtt` | 已发布 | 增加远程连接、失败和恢复中文文案 | 增加同窗口 QoS 1 相邻重复事件保护，保留故障状态流转 | 不覆盖 `v1.1.0`；继续复用原数据卷和私有 HiveMQ 配置 |
| `v1.1.2-remote-mqtt` | 已发布 | MQTT状态页可见时每秒刷新，隐藏时暂停、返回时立即更新 | 无业务逻辑变化，后端同步构建同标签 | 仅更新镜像不会自动修改群晖，需重建前后端服务 |
| `v1.1.3-remote-mqtt` | 已发布 | MQTT状态页和日志页统一可靠刷新、禁用缓存、按更新时间展示 | 设备日志入口兼容 `WARN` 并规范为 `WARNING` | 现有固件无需重刷；继续保持 QoS 1 |
| `v1.1.4-remote-mqtt` | 已发布并完成群晖升级 | 日志详情随列表实时同步；增加本地AI未发现/回退官方AI中文显示 | 增加详情同步所需查询兜底和事件中文映射 | 继续复用原 MySQL 卷和远程私有配置 |
| `v1.1.5-remote-mqtt` | 已发布并完成群晖升级 | 日志首次请求失败后轮询可自愈；顶部显示后端运行时长 | 增加轻量 `/api/system/runtime` 接口 | 页面恢复、聚焦或网络恢复时自动补刷 |
| `v1.1.6-remote-mqtt` | 已发布并完成群晖升级 | 无主要业务页面变化；通过 Compose 等待后端健康后启动 | 增加 Actuator内部健康检查，只监听容器回环 `127.0.0.1:8081` | 不映射管理端口；后端 `healthy` 后前端才启动 |
| `v1.1.7-remote-mqtt` | 已发布并完成群晖升级复测 | 顶部动态显示“检测中/已接入/未连接”，后端恢复后自动重试 | 恢复事件可跨30秒窗口关闭最新未恢复MQTT故障，写入使用事务 | 停止/启动、43条旧日志保留及 `PENDING → RESOLVED → PENDING` 已实测 |
| `v1.1.8-remote-mqtt` | **已发布并完成回退验收** | 无功能变化；不含OpenAPI与新版错误追踪 | 接入 Flyway；空库执行 V1，旧库保留数据并建立 V1基线 | 从1.1.9回退后47条日志保留；MQTT`1/1/0`，新增ID48且总数48 |
| `v1.1.9-remote-mqtt` | **已发布；已完成回退演练** | 包含OpenAPI、统一错误追踪、共享轮询控制器及对应回归 | 增加MQTT Topic/Payload一致性校验、Tomcat 10.1.57安全修复及完整回归入口；数据库仍为Flyway V1 | 首次升级新增ID47；回退1.1.8新增ID48；再升级后48条保留，MQTT`2/2/0`、新增ID49且总数49 |
| `v1.2.0-remote-mqtt` | **已发布；群晖最终运行版本并完成验收** | 增加设备中心、设备独立日志、采集数据表与趋势图；MQTT页面可管理远程凭据 | Flyway V2增加设备监控模式；凭据私有文件持久化并立即重连；正常切换官方AI的事件统一为`INFO/RUNNING/RESOLVED` | GHCR/TCR前后端已发布，漏洞门禁、来源证明、Release、回归和digest一致性校验通过；群晖确认数据保留、独立日志刷新和凭据重连/重启持久化 |
| `v1.2.1-remote-mqtt` | **GHCR/Release/TCR 已发布；已用离线包更新群晖** | 固定 Opus 主动播报、提醒 API/排程、MCP 审计与前端记录 | Flyway V3～V6；下行 announcement/ACK、提醒幂等与固定音频播报 | GHCR 前后端、漏洞门禁与来源证明成功；TCR 后端最初直接复制遇 HTTP/2 `PROTOCOL_ERROR`，后改为 Runner 本地拉取/推送、最多三次重试，并以可运行镜像 config digest 验证。Flyway 回归测试已由旧表数 7 修正为 11 并以 MySQL 8.4 验证通过 |
| `v1.2.2-remote-mqtt` | **GHCR/Release 已发布；离线包已生成；待群晖验收** | 固定 Opus 下行协议修复 | manifest 的 `createdAt`、`expiresAt` 改为 UTC ISO-8601 `Z`；不改变 MQTT Topic、音频帧、提醒表或既有功能 | `v1.2.1` 无时区本地时间导致固件静默拒绝并无 ACK；修复已通过后端回归和 GHCR 发布，待导入群晖后以固定直发和一分钟提醒复验 |
| `v1.2.3-remote-mqtt` | **GHCR/Release 已发布；群晖实机验收通过** | manifest UTC 时钟修复 | 下行 `createdAt`、`expiresAt` 直接由同一个 `Instant` 生成，过期时间固定为 5 分钟 | 群晖导入后，设备以正确 UTC epoch 接受任务，处理 35 个裸 Opus 帧并回传 `received`/`played`；固定中文语音已实际从扬声器播出 |

`v1.2.2` 已发布该 UTC manifest 修复；此前 `v1.2.1` 使用无时区本地时间，固件按协议拒绝且不会产生 ACK。

版本状态规则：只有固定标签、GitHub Release、前后端 GHCR 镜像清单均完成后才标记“已发布”；只有群晖实际拉取并完成数据、API、MQTT和页面检查后才标记“完成群晖升级”。源码提交或分支镜像不能代替固定版本发布。

### v1.2.0 发布记录（2026-07-25）

设备中心和远程 HiveMQ 页面凭据管理已以固定标签 `v1.2.0-remote-mqtt` 发布；GitHub标签发布的前后端GHCR构建、漏洞门禁、来源证明和Release均成功，分支回归、发布镜像复扫和腾讯云TCR同步也均成功。TCR前后端已逐一验证与GHCR源镜像的顶层digest一致；群晖已完成以下适用验收：

- 设备工作台按设备展示独立日志；采集型设备增加数据表、趋势图和后续 AI 分析占位。
- Flyway V2 仅为 `devices` 增加 `monitoring_mode`，已有设备默认 `LOG_ONLY`。
- MQTT 页面可保存远程 HiveMQ 用户名和密码，密码不回显，写入 `docker/local` 私有文件后立即重连。
- 远程 Compose 增加私有目录挂载，镜像或容器重建后继续使用页面保存的凭据。
- `local_ai_discovery_failed` 和 `local_ai_fallback_to_official` 只表示已正常使用官方AI，即使固件上报`WARN/ERROR`也规范为`INFO/RUNNING/RESOLVED`；真实本地AI连接失败和官方AI协议错误仍保留警告或错误。
- 后端32项常规测试通过；2项Flyway测试已在隔离MySQL 8.4.10中真实执行且0跳过，确认空库V1+V2和旧库设备/日志/上报数据保留。前端4项测试和生产构建通过，部署与镜像策略检查通过。
- 群晖确认原数据保留、设备独立日志自动刷新、页面保存凭据后的立即重连与后端重启后持续生效。当前没有采集型设备，因此遥测数据表和趋势图不作为实机验收项。

### 镜像安全与回滚规则（2026-07-23）

- 发布顺序固定为候选镜像构建、Grype漏洞门禁、GHCR推送、digest来源证明；CRITICAL且已有修复版本时不得发布。
- 已发布镜像复扫工作流从远程GHCR Compose解析当前固定标签；当前分支push验收已确认`v1.1.9-remote-mqtt`前后端扫描通过。每周和手工指定历史标签需要该工作流进入GitHub默认分支后启用。
- 使用 `gh attestation verify oci://ghcr.io/yyj7890/<image>:<tag> -R yyj7890/AIoT-Log-System` 验证镜像由本仓库GitHub工作流构建并签名。
- 群晖升级前记录当前前后端固定标签并备份数据库；升级和回退时前后端必须使用同一个标签，不执行`down -v`，继续复用原命名卷、私有HiveMQ配置和端口映射。
- 当前数据库只有Flyway V1，应用回退可继续复用该结构。以后若某个版本引入不向后兼容的V2/V3迁移，回退旧应用时必须同步恢复升级前数据库备份。
- 国内同步源使用腾讯云TCR个人版：`ccr.ccs.tencentyun.com/aiot-log-system/aiot-log-backend`和`aiot-log-frontend`。只从GHCR复制固定标签，不重新构建、不使用`latest`；同步后必须验证两边顶层digest一致。发布凭证只存GitHub Secrets。

### v1.1.6 后端容器健康检查（2026-07-20）

- 后端接入 Spring Boot Actuator，只暴露不含组件详情的 `GET /actuator/health`。
- 管理服务只监听 `127.0.0.1:8081`，Docker 不映射该端口；同一 Docker 网络中的其他容器也无法访问。
- 源码局域网、源码远程和远程 GHCR Compose 均使用后端内部健康检查，前端等待 `service_healthy` 后启动。
- 远程前后端镜像构建通过，Maven 17 下 14 项后端测试全部通过；隔离容器内健康端点返回 `UP`。
- 提交 `c853d6c`、标签、GitHub Release 和前后端 GHCR `v1.1.6-remote-mqtt` 镜像均已发布，镜像清单可正常解析。
- 远程 GHCR Compose 已固定引用 `v1.1.6-remote-mqtt`；群晖已按原项目升级并复用数据库卷、私有环境文件和既有宿主机端口映射，后端容器健康检查通过。

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
- 两个 GHCR/TCR 包以固定标签区分版本，完整差异和发布/群晖状态统一维护在本文“统一版本变更总表”。最新发布和群晖最终运行版本均为`v1.2.0-remote-mqtt`。旧远程标签保留用于回退，避免误用 `latest`；`latest` 只允许 `main` 分支推送更新并保持局域网语义，版本标签事件不得覆盖它。
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
- JDK 17 Maven 测试/编译通过；固件对象编译、完整主组件归档、ELF 链接、BIN 生成和分区检查通过。2026-07-20 用户确认小智固件相关修复均已完成，原固件烧录、MQTT 假异常、启动早期时间、后台重连和重复启动事件待办关闭，不再列入本项目后续路线。

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

## 15. 2026-07-17 日志详情实时同步与本地 AI 回退中文化

### 日志详情实时同步

- 日志列表仍按页面可见时每 1 秒轮询，隐藏标签页时暂停，返回时立即更新。
- 详情弹窗打开后，列表查询若返回同一日志 ID，会原位更新 `currentLog`，因此运行汇总在 30 秒窗口内追加的新事件会直接出现在已打开的弹窗中，不需要关闭后重新打开。
- 原位更新不会关闭弹窗，也不会重置弹窗滚动、筛选、分页或批量选择状态。
- 当前详情记录不在当前页时，使用 `GET /api/logs/{id}` 获取最新数据。详情请求带时间戳和禁止缓存头，并与列表接口一样受 Nginx `no-store` 保护。
- 详情请求使用独立互斥和排队标志，且响应返回时再次核对日志 ID 和请求版本，避免每秒重复并发、快速切换详情时旧响应覆盖新详情。
- 后台详情请求采用静默错误模式：临时网络失败不关闭详情、不阻断列表自动刷新，也不会每秒弹出错误。后端业务码为 `404` 时，页面安全关闭详情并提示“该日志已被删除”。

### 本地 AI 回退事件

- `local_ai_discovery_failed` 显示为“未发现本地 AI 服务”。
- `No valid local AI discovery response this boot` 显示为“本次启动未发现本地 AI 服务”。
- `local_ai_fallback_to_official` 显示为“已回退官方 AI”。
- `Local AI unavailable; official AI connected` 显示为“本地 AI 不可用，已回退官方 AI”。
- 后端负责新入库记录的中文化，前端同时兼容数据库中已有的英文标题和消息。
- 这两类事件描述本地 AI 未发现后正常使用官方 AI，不属于远程日志 MQTT 故障；不修改官方 AI 协议、日志 MQTT 状态、QoS 1 或故障 `PENDING`/恢复 `RESOLVED` 流转。其日志状态仍由固件发送的等级决定。

### 验证与版本边界

- JDK 17 Maven 测试通过：12 项测试、0 失败，覆盖新增两类事件的标题/消息映射，以及原有远程 MQTT 三种映射、`WARN` 兼容、相邻重复抑制、启动批次和故障状态流转。
- 前端 TypeScript 检查和 Vite 生产构建通过。
- Markdown 本地链接检查、敏感信息扫描和 `git diff --check` 全部通过。
- `v1.1.4-remote-mqtt` 发布时，`docker-compose.remote.ghcr.yml` 固定引用该标签。提交 `9ee4379`、同名 Git 标签、GitHub Release 和前后端 GHCR 镜像均已发布成功；旧远程标签和局域网 `v1.0.0-lan` 保留，`latest` 未被远程标签覆盖。
- 群晖已成功拉取两个 `v1.1.4-remote-mqtt` 镜像并完成现有远程项目升级，继续复用原 MySQL 数据卷和私有 `docker/local/hivemq-remote.env`。部署记录不包含真实域名、凭证、NAS 地址或运行 Payload。
- 本轮不修改小智固件，不读取真实 HiveMQ 配置，不修改数据库数据。后续仍需浏览器强制刷新，并使用真机验证详情实时追加、跨页详情兜底和两条本地 AI 中文显示。

## 16. 2026-07-20 日志轮询自愈与系统运行时长

### 问题与根因

- 群晖持续运行两天后观察到：设备日志已经入库，但日志管理页没有显示新记录；进入 MQTT 状态页再返回后，积压日志立即出现，后续重启设备又能正常实时刷新。
- 该现象说明设备、HiveMQ、后端订阅和数据库链路正常，问题位于前端日志页生命周期。
- `LogListView` 原先在 `onMounted` 中依次等待设备/标签选项和首次日志请求，完成后才创建每秒轮询和可见性监听。任一初始请求失败都会使异步挂载钩子提前退出，轮询永远不会创建；切换菜单会重新挂载页面，因此暂时恢复。

### 修复

- 页面挂载时先注册 `visibilitychange`、`focus`、`online`、`pageshow` 监听并启动每秒轮询，再异步加载选项和日志；首次请求失败不再中断轮询生命周期。
- 页面重新可见、窗口重新聚焦、网络恢复或浏览器从缓存恢复时，重新创建定时器并立即刷新；请求互斥继续避免并发堆积，筛选和分页状态不被重置。
- 该修复仅影响前端获取数据的时机，不修改小智固件、HiveMQ、MQTT QoS 1、Topic、后端入库和数据库数据。

### 系统运行时长

- 新增只读 `GET /api/system/runtime`，返回 Spring Boot JVM 的启动时间戳和已运行秒数，不查询数据库，也不暴露主机、网络或凭证信息。
- 所有管理页面顶部显示“系统运行时长”；前端每秒本地递增，每 60 秒与后端校准，并在页面恢复、窗口聚焦或网络恢复时立即同步。
- 该数值代表后端服务进程运行时长，后端容器重启后重新计时，不等同于群晖开机时长。

### 验证与版本边界

- JDK 17 Maven 测试通过：14 项测试、0 失败，其中新增 2 项运行时长计算和非负边界测试；原有 12 项远程日志回归测试继续通过。
- 前端 TypeScript 检查和 Vite 生产构建通过。
- 本轮变更文件敏感信息扫描、29 个 Markdown 文件的本地链接检查和 `git diff --check` 全部通过。
- 提交 `d0e6d4f`、独立标签、GitHub Release 以及前后端 GHCR `v1.1.5-remote-mqtt` 镜像均已发布，两个公开镜像清单已验证可解析；旧远程标签和局域网 `v1.0.0-lan` 保留，`latest` 未被远程标签覆盖。
- `docker-compose.remote.ghcr.yml` 已固定引用 `v1.1.5-remote-mqtt`；群晖已在原远程项目中升级前后端，继续复用原 MySQL 数据卷和私有环境文件。
- 浏览器强制刷新后已确认系统运行时长逐秒增加、MQTT 保持已连接；停留在日志管理页重启小智时，新日志无需切换 MQTT 状态页即可约 1 秒自动出现，浏览器标签隐藏后返回也会立即补刷。
- 后续仍需进行长时间观察，并在后端正常重启时确认运行时长从零重新开始；首次 API 请求失败场景出现时确认轮询能够自行恢复。

## 17. 2026-07-21 群晖重启与真实故障状态流转验收

### 重启与停止/启动

- 后端连续运行约 20 小时后执行正常重启，API、远程 MQTT TLS 连接、日志页轮询和运行时长均自行恢复；运行时长从零重新计时，原 40 条日志和 MySQL 数据卷保持不变。
- 手动停止后端时，API 不可达，前端静态页面仍可打开并显示请求失败；再次启动后，API、MQTT 和日志列表无需切换菜单即可恢复。
- 顶部“后端已接入”当前是硬编码标签，后端停止时仍会显示，属于待修复的在线状态误报。运行时长本身已正常恢复；窄窗口中由响应式 CSS 隐藏，不属于故障。

### 故障、恢复、再次故障

- MQTTX 依次发送 `mqtt_connection_failed`、`mqtt_reconnected`、`mqtt_connection_failed`，后端收到/处理成功/处理失败计数为 `3/3/0`；新记录按时间依次为 `PENDING`、`RESOLVED`、`PENDING`。
- 第一次失败和恢复相隔约 75 秒，超过 30 秒运行日志汇总窗口。恢复事件因此新建一条 `RESOLVED` 记录，旧失败记录仍为 `PENDING`，说明现有实现只保证同一汇总窗口内的故障状态流转。
- 后续修复应保留 30 秒事件聚合和相邻去重，同时独立查找对应的最新未恢复 MQTT 故障，使跨窗口恢复也能关闭旧故障；需要增加跨窗口单元测试和群晖实测。
- 本次只完成检测和文档记录，没有修改小智固件、HiveMQ 配置、程序代码或既有数据；三条测试日志仍保留在数据库中。

### 本地修复与验证

- 前端顶部改用 `/api/system/runtime` 每 5 秒探测后端状态；请求失败显示“后端未连接”并清空过期运行时长，成功后自动恢复“后端已接入”。
- 后端恢复事件会独立查询同设备最新的 `PENDING` MQTT 连接故障并关闭，再执行原 30 秒汇总逻辑；整个处理加入事务，保留原有 QoS 1 相邻去重和启动批次边界。
- 新增跨窗口恢复用例后，JDK 17 Maven 15 项测试全部通过；前端类型检查和生产构建通过。
- 提交 `7660f48`、标签、GitHub Release 和前后端 GHCR `v1.1.7-remote-mqtt` 镜像均已发布，两个公开镜像清单已验证可解析；群晖尚未升级，`v1.1.6-remote-mqtt` 仍保留原行为。
