# 当前项目状态

更新时间：2026-07-27

## 当前阶段

### 2026-07-27：固定 Opus MQTT 主动播报后端源码（真实固定语音联调通过）

- 后端在既有 Paho MQTT 连接中新增 `aiot/device/+/announcement/ack` 订阅；既有 `report`、`log` 订阅、计数和入库逻辑保持不变。
- 新增固定测试播报发布链：先以 QoS 1、`retain=false` 发布 `aiot/device/{deviceCode}/announcement/command` manifest，再按帧号递增发布 `aiot/device/{deviceCode}/announcement/audio/{taskId}/{frameIndex}` 原始二进制 Opus packet。
- manifest 固定使用 `aiot-announcement-v1`、16 kHz、单声道、60 ms Opus packet stream，并为每一帧生成 8 位十六进制 CRC32。
- 新增 Flyway V3 的播报任务与 ACK 时间线持久化；只接受 `received`、`played`、`failed`，按任务和状态幂等去重，安全保存失败原因。
- `POST /api/announcements/fixed-test?deviceCode=...` 仅用于后续开发联调，默认由 `ANNOUNCEMENT_TEST_ENABLED=false` 禁用；测试资源仅由固件已有 `zh-CN/welcome.ogg` 本地拆分为 35 个裸 Opus packet，不调用或部署 TTS。
- 真实联调使用 `tools/run-announcement-local-test.ps1` 读取被忽略的远程环境文件并临时启用测试开关；该脚本为本机源码验证使用独立 MQTT Client ID，不修改群晖配置或私有凭证文件。
- 本地 Maven 测试 39 项通过、2 项既有 MySQL Flyway 集成测试因未提供 MySQL 而跳过；首次发送时设备未在线，任务仅记录为 `PUBLISHED`。设备启动后，使用独立本机 MQTT Client ID 的受控复测收到并持久化最新任务的 `RECEIVED → PLAYED` 时间线，确认 HiveMQ 下行、设备自动播放及 ACK 回传已打通；未部署 TTS/群晖、未烧录设备。

### 2026-07-27：提醒核心源码（未部署）

- 新增 Flyway V4 `reminders` 表和 `/api/reminders` 创建、查询、取消 API；提醒保存目标设备、文本、到期时间、状态和对应播报任务号。
- 到期扫描由 `REMINDER_SCHEDULER_ENABLED=false` 默认关闭；启用时用数据库状态抢占避免重复发布，到期仅调用已验证的固定 Opus 播报器，提醒文本暂不转语音。
- 本机受控联调已创建一条一分钟后提醒，确认 `SCHEDULED → PUBLISHED`，并收到关联播报的 `RECEIVED → PLAYED` ACK。当前不提供 MCP 工具、不部署 TTS；`tools/run-announcement-local-test.ps1 -EnableReminderScheduler` 只为本机受控联调显式开启调度，后续需补充取消、失败和重复场景测试，再接入官方小智提醒 MCP。
- 取消边界已受控验证：创建后立即取消的一分钟提醒，在原定时间过后仍为 `CANCELED` 且没有关联播报任务。

2026-07-25 `v1.2.0-remote-mqtt` 已发布并完成群晖验收：设备通过 Flyway V2 新增 `monitoring_mode`，旧设备默认 `LOG_ONLY`，传感器类设备可选择 `TELEMETRY`。前端侧栏以“设备中心”为主入口，设备列表整行可打开设备工作台；工作台只查询当前 `deviceId` 的日志，并按设备展示概览、分页日志、采集数据、趋势可视化和后续 AI 分析占位。原全局 `/logs` 路由保留用于兼容管理，但不再出现在侧栏。远程 HiveMQ 用户名和密码可在 MQTT 页面保存并触发后端立即重连；接口只返回用户名及密码配置状态，密码保存在宿主机 `docker/local/hivemq-remote-credentials.properties` 私有文件，不写数据库、不回显、不进入仓库。`local_ai_discovery_failed`与`local_ai_fallback_to_official`表示系统已正常切换官方AI，现统一规范为`INFO/RUNNING/RESOLVED`，真实AI连接或协议错误仍保留原告警。标签发布的前后端GHCR构建、漏洞门禁、来源证明和GitHub Release均成功；分支回归、已发布镜像复扫和腾讯云TCR同步均成功，TCR前后端均完成源与镜像digest一致性校验。群晖确认原数据保留、设备独立日志自动刷新、页面保存凭据立即重连及后端重启后继续生效；当前没有采集型设备，因此遥测表与趋势图不作虚构实机结论。根目录 README 已补充可复制的腾讯云TCR `docker pull` 指令。前三阶段至此全部完成，下一阶段才是AI日志与遥测分析。

本地远程源码版现已运行于`http://127.0.0.1/`。启动时发现本地与群晖使用相同 MQTT Client ID 会互相断开，已把本地源码Compose改为`aiot-log-backend-remote-local-source`，群晖成品镜像Compose不变。重新创建后前端200、后端healthy、MySQL V2成功、远程MQTT连接稳定；当前凭据仍来自私有环境文件，页面保存、即时重连和重启持久化尚待人工验收。

2026-07-15：`Remote-Hivemq` 分支已实现 HiveMQ Cloud 远程 MQTT 模式，且未替换默认局域网模式。后端可通过 `MQTT_MODE=remote` 使用 Paho `ssl://` TLS 订阅原有 `report`/`log` Topic，远程时强制关闭 UDP `19830` 响应器、隐藏远程 Broker 地址并禁止页面修改本地 Mosquitto 凭证。新增源码/GHCR 远程 Compose、Windows 启停脚本、群晖私有 `docker/local/hivemq-remote.env` 初始化和远程镜像部署包生成选项；该文件与局域网 `.env` 分离。远程编排不包含 Mosquitto、不映射 `1883` 或 UDP `19830`。首个远程版以 GHCR 标签 `v1.1.0-remote-mqtt` 发布；2026-07-16 的状态中文文案和 QoS 1 相邻重投修复独立发布为 `v1.1.1-remote-mqtt`，MQTT 状态页自动刷新独立发布为 `v1.1.2-remote-mqtt`，`WARN` 等级兼容和状态/日志列表 1 秒刷新独立发布为 `v1.1.3-remote-mqtt`；2026-07-17 已发布 `v1.1.4-remote-mqtt`，增加详情弹窗实时同步和本地 AI 回退事件中文化。旧版继续保留用于回退。局域网版固定为 `v1.0.0-lan`；`latest` 保持局域网语义并只由 `main` 分支推送更新。小智独立日志客户端已增加持久化远程 TLS 配置、ESP-IDF CA bundle、域名验证/SNI 和跳过 UDP 发现，未修改官方 AI、OTA 或 WebSocket 通道。真实 HiveMQ 信息未读取或写入项目。

远程版本已完成的外部前置验证：用户使用 MQTTX 成功验证 TLS `8883` 连接、`aiot/device/#`（QoS 1）订阅，以及向 `aiot/device/TEST-001/log`（QoS 1）发布并接收 JSON 测试日志；未记录真实域名或凭证。

远程版本已完成 Windows 端到端验收：远程 Compose 的 MySQL、后端和前端均已启动，后端状态页显示远程 TLS 已连接。MQTTX 发布的有效 `/log` 消息已由 Spring Boot 成功处理并写入新建 MySQL 数据库；测试同时确认 Topic、Payload `deviceCode` 与已创建设备编号必须一致，`logType` 必须使用 `RUNNING`、`ERROR`、`MAINTENANCE` 或 `INSPECTION`。

远程版本已完成群晖实机验收：使用固定标签 `v1.1.0-remote-mqtt` 在 DSM Container Manager 创建独立远程项目，MySQL、后端和前端启动成功，后端通过 HiveMQ TLS 连接后收到了 MQTTX 测试消息并处理成功。NAS 的 Docker CLI 不带 Compose V2 插件，因此通过 Container Manager 创建项目；宿主机 `8080` 被既有服务占用后，将远程后端和前端宿主机端口分别调整为 `18080` 和 `18000`，容器内部 `backend:8080` 通信不变。未记录真实 NAS 地址或 HiveMQ 凭证。

2026-07-16 小智 ESP32 真机已成功通过 HiveMQ TLS 上传日志，确认固件、HiveMQ、后端订阅和入库链路可用。首次运行汇总出现“连接失败 → 恢复 → 已连接”的假异常：固件把等待可信时间和主动断开误记为失败，后端又把包含一次 `ERROR` 的 30 秒汇总保持为严重/待处理。固件 `.2` 修复版已区分延迟、真实失败和主动停止，修正重复连接事件并支持空队列主动重连；后端收到明确 MQTT 恢复事件后改为 `RESOLVED`，后续新故障会重新置为 `PENDING`。2026-07-20 用户确认小智固件相关修复均已完成；局域网 UDP 回归按当前安排暂存跳过。

2026-07-16 IoT 展示与幂等修复：后端和前端均已增加远程日志 MQTT TLS 的连接成功、失败重试、连接恢复中文映射，页面连接成功文案为“远程日志 MQTT（TLS 8883）已连接”。同一启动批次、同一 30 秒汇总窗口内，如果新事件与当前汇总最后一条标准化摘要完全相同，则不再重复追加或增加计数；不同事件仍正常追加，`startup`/`firmware_started` 仍强制新建启动批次。该保护面向 QoS 1 可能发生的相邻重投，不降低 QoS，也不改变 `mqtt_connected`、`mqtt_connection_failed`、`mqtt_reconnected` 事件类型。故障 `PENDING`、恢复 `RESOLVED`、后续新故障重新 `PENDING` 的既有逻辑保持不变。

2026-07-16 MQTT 状态与日志列表刷新改进：两个页面在可见时统一每 1 秒更新；浏览器标签隐藏时停止轮询，重新返回页面时立即刷新。请求增加互斥和禁止缓存参数，日志列表的自动请求不再反复显示表格加载遮罩，并按最近更新时间排序，使运行日志在 30 秒汇总窗口内被追加时也会及时回到顶部。手动“刷新”按钮继续保留。远程 Broker、Topic、QoS 和凭证仍按安全设计只读展示，真实远程域名和凭证不会进入页面。

2026-07-16 日志等级兼容修复：小智固件使用常见等级 `WARN`，IoT 枚举使用 `WARNING`，此前链路虽然已通过 HiveMQ TLS 正常到达后端，但入库校验会报“日志等级不合法”。后端设备运行日志入口现按大小写规范化等级并将 `WARN` 映射为 `WARNING`；这是协议枚举兼容问题，不是 HiveMQ、TLS 或网络故障，现有已烧录固件无需因该问题重新烧录。

2026-07-17 日志详情与本地 AI 回退显示修复：打开详情时主动读取最新详情；列表轮询获得同 ID 记录后原位更新 `currentLog`，不关闭弹窗，也不重置滚动、筛选、分页或批量选择。当前记录不在本页时，前端使用带防缓存参数的详情接口静默兜底，请求互斥并校验请求版本，避免重复并发和旧响应覆盖；业务码 404 时关闭详情并提示记录已删除，普通网络失败不影响列表轮询。后端和前端同时增加 `local_ai_discovery_failed`、`local_ai_fallback_to_official` 及其英文消息的中文映射；这些事件表示本地 AI 不可用后正常使用官方 AI，不修改官方协议、日志 MQTT 状态或 MQTT 故障恢复逻辑。

2026-07-20 日志轮询自愈与系统运行时长：两天运行观察发现，日志页首次挂载时会先等待设备/标签选项和日志列表请求，任何一次初始请求失败都会使挂载钩子提前退出，导致每秒轮询及可见性监听从未创建；切换到 MQTT 状态页再返回会重新挂载，因此积压日志立即出现。修复后，轮询和 `visibilitychange`、`focus`、`online`、`pageshow` 恢复监听先于网络请求建立；日志初始请求失败会被请求层提示但不会终止轮询，页面恢复时会重建定时器并立即刷新。顶部状态区新增 Spring Boot 后端运行时长，后端提供不查询数据库的轻量运行时接口，前端每秒显示并每分钟校准；该计时在后端进程重启后重新开始，不代表 NAS 开机时长。

构建、发布与部署：提交 `d0e6d4f` 已推送到 `Remote-Hivemq`，独立标签、GitHub Release 以及前后端 GHCR `v1.1.5-remote-mqtt` 镜像均已发布并验证可解析。群晖已在原远程项目中升级前后端到 `v1.1.5-remote-mqtt`，继续复用原 MySQL 数据卷和私有环境文件；强制刷新后，系统运行时长逐秒增加，MQTT 保持已连接，小智重启产生的新日志无需进入 MQTT 状态页即可约 1 秒自动显示，浏览器标签隐藏后返回也会立即补刷。JDK 17 下 14 项后端测试全部通过，其中新增 2 项系统运行时长边界测试；前端 TypeScript 检查和 Vite 生产构建通过，敏感信息扫描、29 个 Markdown 文件的本地链接检查和 `git diff --check` 也已通过。小智固件、HiveMQ TLS、QoS 1、Topic、数据库卷和私有配置均未修改。

2026-07-20 工程质量版本 `v1.1.6-remote-mqtt`：后端增加 Spring Boot Actuator，管理端口仅监听 `127.0.0.1:8081`，只公开不含组件细节的 `health` 端点且不映射至宿主机。源码构建的局域网与远程 Compose 增加后端内部健康检查，前端等待 `service_healthy` 后启动；远程 GHCR Compose 已固定到 `v1.1.6-remote-mqtt`。远程前后端镜像构建、JDK 17 Maven 14 项后端测试、Compose 结构校验和隔离容器验收均已通过；健康端点返回 `UP`，同一 Docker 网络中的其他容器不能访问内部 `8081`。提交 `c853d6c`、标签、GitHub Release 和两个 GHCR 镜像均已发布。群晖已在原远程项目中升级前后端到该版本，继续复用原 MySQL 数据卷和私有环境文件；前端、后端和 MySQL 均正常运行，后端容器状态为 `healthy`，宿主机端口映射保持不变。

2026-07-21 群晖长时间运行、重启与状态流转实测：后端已连续运行约 20 小时，正常重启后 API、远程 MQTT TLS、日志轮询和运行时长自动恢复，运行时长从零开始，40 条既有日志保留；手动停止期间 API 不可达而前端静态页仍可访问并显示请求失败，重新启动后页面无需切换菜单即可恢复。MQTTX 依次发送失败、恢复、再次失败事件，后端计数为收到/处理成功/失败 `3/3/0`，对应新记录状态为 `PENDING`、`RESOLVED`、`PENDING`。本轮同时发现两项待修问题：顶部“后端已接入”是硬编码，后端停止时仍误报；失败与恢复间隔超过 30 秒时，恢复事件因超出运行日志汇总窗口而新建 `RESOLVED` 记录，原失败记录仍为 `PENDING`。运行时长在窄窗口中因 CSS 隐藏而不可见，不是恢复故障。

2026-07-21 修复与发布完成：`AppHeader` 现在使用 `/api/system/runtime` 作为轻量在线探针，初始显示“后端检测中”，成功显示“后端已接入”，失败清空过期运行时长并显示“后端未连接”，每 5 秒及页面恢复时自动重试。后端收到 MQTT 恢复事件时，会在 30 秒事件汇总之外独立查找并关闭最新对应的未恢复 MQTT 故障；关闭旧故障与写入恢复事件处于同一事务。新增跨窗口恢复测试后，JDK 17 Maven 共 15 项测试全部通过；前端 TypeScript 检查和 Vite 生产构建通过。提交 `7660f48`、标签、GitHub Release 和前后端 GHCR `v1.1.7-remote-mqtt` 镜像均已发布并验证清单可解析；发布当日群晖仍运行 `v1.1.6-remote-mqtt`。

2026-07-22 群晖 `v1.1.7-remote-mqtt` 原地升级与复测通过：保留原 MySQL 数据卷、私有配置和端口映射。停止后端时运行时接口连续三次超时而前端静态页保持 HTTP 200；重新启动后运行时长从 16 秒持续增长，远程 MQTT TLS 自动连接，原 43 条日志全部保留。跨窗口恢复消息使前一天最新故障 ID 43 从 `PENDING` 变为 `RESOLVED`，新增恢复记录 ID 44 为 `RESOLVED`；再次失败新增 ID 45 为 `PENDING`。本次 MQTT 计数为收到/处理成功/失败 `2/2/0`，确认“失败 → 跨窗口恢复 → 再失败”状态流转、数据卷复用和服务恢复正常。ID 41 是旧版本遗留的更早故障，按“只关闭最新对应故障”规则保留不变。

2026-07-22 Flyway 无损数据库升级完成并发布、部署为 `v1.1.8-remote-mqtt`：后端加入 `flyway-core`、MySQL 支持和 V1 初始迁移，旧数据库通过版本 1 基线接入；以后结构变化只能新增 V2、V3 迁移。隔离 MySQL 8.4 验证中，空库创建 7 张业务表并记录 `V1/SQL/success`，已有库的测试设备保留且只记录 `V1/BASELINE/success`。15 项后端测试、生产 JAR、四套 Compose 和两种群晖部署包均通过检查。群晖原项目升级后出现 `JdbcTableSchemaHistory` 和 `DbBaseline` 成功记录，原45条日志与设备数据保留；MQTT升级验证消息收到/处理/失败为 `1/1/0`，新增 ID 46，日志总数变为46。固定镜像版本差异统一见 `hivemq-remote-mqtt-version.md` 的“统一版本变更总表”。

2026-07-23 OpenAPI/Swagger 源码接入完成：依据 Spring Boot 3.3.x 兼容范围使用 Springdoc 2.6.0，生成中文标题、说明和版本元数据；`aiot-api` 分组仅收录 `/api/**`，9 个业务控制器均有中文 Tag 和 Operation，通用 `ApiResponse`、`PageResult` 已补充 Schema。接口 JSON 为 `/v3/api-docs/aiot-api`，调试页面为 `/swagger-ui.html`，可用 `OPENAPI_ENABLED` 和 `SWAGGER_UI_ENABLED` 关闭。新增配置检查和真实随机端口集成测试，确认两个端点均返回 HTTP 200 且主要接口存在；JDK 17 Maven 共 19 项测试全部通过，生产 JAR 构建成功。该功能已随 `v1.1.9-remote-mqtt` 发布并在群晖完成真实端点验收。

2026-07-23 统一错误与日志规范完成：新增集中式 `ErrorCode`，所有业务异常不再使用裸数字；错误响应的数值 `code` 与 HTTP 状态一致，同时提供稳定字符串 `errorCode` 和请求 `traceId`。`X-Trace-Id` 会在响应头返回并进入 MDC；合法的调用方追踪号可继续沿用，否则后端生成新值。全局异常处理覆盖业务错误、字段校验、参数类型、损坏 JSON、不支持的方法、不存在路由、数据库冲突和未知异常，未知异常不向客户端暴露堆栈。可预期拒绝统一为 `api_request_rejected`，内部业务操作失败为 `api_operation_failed`，未知错误为 `api_unhandled_error`，均使用键值字段且不记录请求体和凭证。前端保留数值状态码兼容，并在 `ApiRequestError` 中传递 `errorCode`、`traceId`。4项真实端点测试覆盖400/404和追踪号关联，后端总计23项测试、前端生产构建和后端生产 JAR均通过。该功能已随`v1.1.9-remote-mqtt`发布。

2026-07-23 第二阶段自动化回归基础完成：MQTT订阅器新增5项测试，覆盖日志/状态上报路由、QoS、计数、非法JSON、字段校验、Topic设备编号匹配和断线/恢复回调；运行日志服务新增QoS 1重复故障不追加、非MQTT事件不关闭MQTT故障测试，原有跨窗口 `PENDING → RESOLVED → PENDING` 测试继续保留。Flyway新增2项仅在提供真实MySQL时启用的集成测试，分别验证空库执行V1创建7张业务表，以及旧库建立V1基线并保留设备数据。前端接入Vitest并新增4项共享轮询控制器测试，日志页和MQTT状态页均改用该控制器。部署脚本自动验证5套Compose、远程/局域网边界、前后端镜像标签一致、MySQL命名卷和4个停止脚本不删除数据卷。GitHub新增回归工作流，在MySQL 8.4服务上运行完整后端、前端和部署检查。本机结果为后端32项发现、30项通过、2项因无MySQL环境跳过；前端4项通过，生产构建与部署检查通过。相关功能已随`v1.1.9-remote-mqtt`发布。

2026-07-23 第三阶段镜像发布链、国内同步和群晖升级已完成：前后端Dockerfile升级到Maven 3.9.16/JDK 17 Noble、Node 24 Alpine 3.24和Nginx 1.30 Alpine。GHCR工作流先用Anchore Grype阻断已有修复版本的CRITICAL漏洞，通过后推送，并用GitHub官方Artifact Attestation为实际digest生成Sigstore签名的SLSA来源证明。首次运行中后端因Tomcat 10.1.31漏洞被正确阻断；升级Tomcat 10.1.57后完整流程成功。腾讯云TCR同步运行`30001574691`成功，公有镜像顶层digest与GHCR一致并保留attestation manifest。群晖通过TCR原地升级`v1.1.9`后，原46条日志保留并新增ID47。2026-07-24直接回退`v1.1.8`后47条数据保留，测试`1/1/0`、新增ID48；随后再通过TCR升回`v1.1.9`，48条数据保留，Swagger、Trace ID和远程MQTT恢复，测试`2/2/0`、新增ID49，总数49。双向切换与每次切换后继续写入均成功，群晖最终运行1.1.9。

当前结论（2026-07-13）：真实小智已完成普通 Wi-Fi、手机热点、断网恢复、本地 MQTT 日志汇总、官方/本地 AI 动态切换和本地 AI WebSocket 实际对话验证。IoT 全局 MQTT 凭证已创建并启用：匿名访问已关闭，服务重启后小智使用配网页保存的同一套凭证完成日志上报验证。

补充验收（2026-07-13）：本地 AI 服务取消/不可用时，设备最终能够自动回退官方 AI；当前已知体验问题是回退等待偏长。本地 AI 回答能力也弱于官方 AI。两项均需在小智固件/本地 AI 服务项目处理（本地发现、WebSocket 连接超时和重试策略；本地 LLM Provider、模型与提示词配置），IoT 端仅负责记录对应状态日志。

补充（2026-07-12）：`Local AI server discovered` 映射为“已发现本地 AI 服务”，只描述发现结果，不能作为实际连接状态；`local_ai_connected` / `Local AI server connected` 才映射为“已连接本地 AI 服务”，`local_ai_connection_failed` / `Local AI server connection failed` 映射为“本地 AI 服务连接失败”。实机曾在本机服务可用时建立 WebSocket 实际对话；本地服务不可用时固件应安全回退官方服务。实机曾在同一 30 秒窗口收到两轮相同启动事件而形成“设备运行上报（8 条）”；IoT 后端未自行重复生成。2026-07-20 用户确认固件相关修复均已完成，该待办关闭。

验收（2026-07-12）：本地 AI WebSocket 实际对话已验证。服务端收到设备 `hello`、`listen`（识别文本“你好小智”）和 MCP 工具协商消息；服务端成功下发并播放“我认真听着呢，请讲。”，同时识别到设备支持 6 个 MCP 工具。该结果确认设备已实际使用本地 AI 通道，而不只是完成发现或 OTA 配置下发。

补充（2026-07-11）：小智运行事件已完成中文化展示。已知事件显示为简短中文标题，例如“设备启动”“Wi-Fi 已连接”“日志 MQTT 已连接”；标题列固定单行，超长内容以省略号和悬浮提示显示。后端负责新日志入库转换，前端兼容转换已存在的英文记录。

补充（2026-07-11，2026-07-13 修正）：为避免一次设备启动连续产生多行日志，后端会按设备合并 30 秒内连续收到的 `/log` 运行事件。一条日志标题显示为“设备运行上报（n 条）”，内容按行保留每一步的最简状态；详情页将其显示为“固件开始初始化 → Wi-Fi 已连接 → 日志 MQTT 已连接 → …”，避免事件标题和消息重复。`startup` 或 `firmware_started` 始终开始一条新的汇总，避免复位发生在窗口内时串入上一轮；其他事件超过 30 秒才新建下一条。可通过 `MQTT_RUNTIME_LOG_MERGE_WINDOW_SECONDS` 调整汇总窗口。

验收（2026-07-11）：`XIAOZHI-001` 实机一次启动的 4 条事件已合并为一条“设备运行上报（4 条）”日志，内容为固件开始初始化、Wi-Fi 已连接、日志 MQTT 已连接、官方 AI 协议已连接或重连。MQTT 状态为已连接，`receivedCount=26`、`handledCount=26`、`failedCount=0`。此前出现的“无法连接服务，请稍后再试”属于官方 AI 通道短暂失败，后续已收到官方协议重连事件。

验收（2026-07-12）：电脑与 `XIAOZHI-001` 同时连接手机热点后，设备成功自动发现局域网 Mosquitto 并继续上报日志。说明自动发现使用热点下动态分配的电脑局域网 IPv4，可在更换普通 Wi-Fi 或手机热点后工作；前提是热点未启用客户端隔离、Windows 网络配置为专用网络，且 TCP 1883 与 UDP 19830 已按专用网络放行。

验收（2026-07-12）：已完成断网后自动重连与日志恢复实机验证。小智网络恢复后重新发现/连接局域网 MQTT Broker，并继续将运行事件上传到 IoT 日志系统；未发现因临时断网导致日志通道长期失效的情况。

显示规则（2026-07-12）：新增日志 MQTT 连接状态中文化。`Log MQTT connection failed and was retried` 显示为“日志 MQTT 连接失败，正在重试”；`Log MQTT connection recovered` 显示为“日志 MQTT 连接已恢复”。前端兼容已存在的英文汇总记录，后端负责新入库记录中文化。

显示规则（2026-07-13）：`Local AI WebSocket hello completed` 及事件类型 `local_ai_websocket_hello_completed` 显示为“本地 AI WebSocket 握手完成”，表示本地 AI WebSocket 已完成初始 hello 握手，不等同于只发现本地服务。

安全加固验收（2026-07-13）：认证方案采用全局设备 MQTT 账号，而非每设备独立账号。已在 MQTT 状态页创建全局账号密码、关闭匿名访问并重启本地服务；小智配网页保存相同凭证后，日志上报继续成功。后端重建本机 Mosquitto 密码文件与通用 `aiot/device/+` Topic ACL，凭证不纳入版本控制。设备留空或错误时不影响设备启动和 AI，仅停止日志上传。

第一阶段管理功能和第二阶段 HTTP/MQTT 自动上报已完成，既有 Docker Compose 部署已验收。系统主体是通用 AIoT 日志管理与设备接入；小智真实硬件作为当前示例，已通过独立 MQTT 日志通道接入本地日志系统，并完成本地 AI 优先、官方 AI 回退的实机验证。GitHub README、Mermaid 架构图、截图规范和发布前安全检查清单已完成；实际展示截图尚待人工脱敏准备。2026-07-13 已将 Docker 配置改为本机私有配置自动生成、匿名 MQTT 关闭和 UDP `19830` 发布，并新增 GHCR 成品镜像工作流与拉取式部署脚本；新增群晖 DSM SSH 初始化脚本及无源码的成品镜像部署包生成脚本，解决 Container Manager 不执行 Windows 脚本和 NAS 无须上传前后端源码的问题。新用户 Docker 首次部署已调整为空白数据库，不再自动导入演示数据；默认关闭发现 Token 以允许未预置 Token 的设备自动发现；Docker MQTT 状态页更新全局凭证时会保留 Mosquitto 安全文件的容器读取权限。2026-07-14 已完成群晖 NAS Container Manager 成品镜像实机部署与设备自动发现连接验收，专题记录见 `synology-nas-deployment.md`。

## 已完成功能

- 设备中心源码改造（尚未发布）：设备可选“仅运行日志”或“日志与采集数据”，点击设备进入独立工作台；日志请求固定当前设备，采集型设备展示数据表和趋势图
- 远程 HiveMQ 页面凭据修改源码（尚未发布）：私有文件持久化、密码不回显、保存后立即重连

- 设备、日志、标签、告警规则管理
- 首页统计、设备详情和日志筛选
- 日志管理页每 1 秒自动刷新当前筛选和分页结果；页面隐藏或离开时停止，返回标签页时立即刷新一次
- 日志管理页在点击“批量删除”后才显示勾选列，支持当前页全选和批量删除；删除前二次确认，并同时删除关联标签关系。完成或取消时会清空跨页保留的表格选择状态，避免再次提交已删除日志编号
- 日志来源：人工录入、设备上报、系统生成
- HTTP 设备上报与查询
- MQTT 订阅、状态监控和模拟上报
- MQTT 运行日志 Topic 消费，写入 `DEVICE` 来源日志
- 异常上报自动生成设备日志
- 上报数据和温度、湿度、电压、信号趋势
- 固定左侧菜单和独立内容滚动
- 本地一键启动/停止
- 本地启动脚本以 200 毫秒间隔轮询服务端口，减少后端启动完成后的额外等待；Java 后端初始化约 2 秒仍属正常
- Docker 四服务一键部署
- 新对话项目上下文与分类文档导航
- GitHub 展示 README、可维护 Mermaid 架构图、截图准备说明和发布前安全检查清单

## Docker 验证

- frontend、backend、mysql、mosquitto 均正常运行
- MySQL 健康检查通过
- 网页和后端接口返回正常
- MQTT 后端连接正常
- 初始化中文乱码已修复
- 启动脚本会等待后端与前端就绪后打开浏览器

脚本：

```text
docker-start.cmd   日常启动
docker-update.cmd  首次部署或代码更新后重建
docker-stop.cmd    停止并保留数据
```

## 当前限制

- 小智 `XIAOZHI-001` 已完成真实局域网 MQTT 联调、普通 Wi-Fi/手机热点切换和断网自动恢复验证；尚未完成长时间稳定性验证
- IOT 已支持 `aiot/device/+/log` 运行日志 Topic；小智固件已发布 `startup`、`wifi_connected`、`mqtt_connected`、`official_protocol_connected` 等真实运行事件并成功入库
- MQTT 已关闭匿名访问；当前仍只适合可信局域网，尚未启用 TLS 和每设备独立 ACL
- ESP32 局域网 MQTT 配置文件与本地启动脚本已准备，Windows TCP `1883` 专用网络入站规则已创建；Mosquitto 已验证局域网监听和本机发布订阅
- IOT UDP `19830` MQTT Broker 发现响应已实现并完成真实小智设备验证；小智自定义固件已自动发现 Broker 并发布状态事件
- `/log` Topic 已通过 `XIAOZHI-001` MQTT 测试事件验证，确认写入 `logs` 表且来源为 `DEVICE`
- 真实小智已发布 `startup`、`wifi_connected`、`mqtt_connected`、`official_protocol_connected` 日志并成功入库；2026-07-20 用户确认小智固件相关修复均已完成，启动早期时间、MQTT 假异常、后台重连和重复启动事件不再列入本项目后续任务
- 已执行 `sql/runtime-log-localization.sql` 以中文化历史运行日志；前端可即时兼容展示英文历史记录。当前运行中的高权限 Java 后端无法由普通会话重启，因此需管理员重启后端后，新入库日志才会直接保存为中文。
- MQTT 全局凭证管理和关闭匿名访问已完成实机验收；HTTP 设备身份认证和管理网页登录尚未实现，P1 安全功能已按当前安排暂存跳过
- 已验证 GHCR 成品镜像在群晖 NAS 拉取和运行；在线演示已随 P1 安全功能一起暂存跳过
- 已建立后端32项、前端4项及5套Compose部署检查的自动化回归基础；真实MySQL 8.4迁移测试由GitHub回归工作流执行

## 测试状态

已验证：

- 后端编译与前端生产构建
- 设备、日志、标签和告警规则基础操作
- HTTP 正常/异常模拟上报
- Mosquitto 真实发布订阅和 MQTT 模拟上报
- 异常上报入库及自动日志
- Docker 四容器启动、网页、API、MySQL 健康和 MQTT 连接
- Docker 中文初始化数据修复
- 既有 Docker 四服务验收完成；最新私有凭证、MQTT 认证与 UDP 自动发现 Docker 配置已在群晖实机验证
- GHCR 前后端成品镜像已在群晖新环境拉取并完成运行验证；MySQL/Mosquitto 因 Docker Hub 网络不稳采用离线导入
- 群晖已原地升级至 `v1.1.7-remote-mqtt`；停止/启动、API 与运行时长恢复、MQTT 自动重连、43 条既有日志保留以及跨窗口 `PENDING`/`RESOLVED`/`PENDING` 均已实测通过
- 群晖已继续升级至 `v1.1.8-remote-mqtt`；Flyway 旧库基线、45 条既有日志保留和升级后新增日志写入均已实测通过
- OpenAPI 分组 JSON 和 Swagger UI 已通过真实 HTTP 端点测试并随 `v1.1.9-remote-mqtt` 完成群晖验收
- 群晖已从`v1.1.9`回退`v1.1.8`；47条数据保留且回退后新增ID48，总数48
- 群晖已再次升回`v1.1.9`；48条数据保留且新增ID49，总数49，最终运行版本确定
- MQTT、状态流转、Flyway、前端轮询和Docker数据卷保护均已有对应自动化回归入口

尚未验证：

- 真实硬件长时间持续上报
- 群晖 Container Manager 中停止、启动全过程的 `healthy` 状态截图留证；API、MQTT、前端可用性、运行时长恢复和数据保留已经验证

暂存跳过：

- P1 管理登录、角色权限、HTTP 设备认证、每设备 MQTT 凭证和公网安全能力
- 多设备并发和大数据量性能测试
- 前端性能专项、Redis 与额外生产 Nginx 配置
- 追加 GitHub 展示和在线演示
- 局域网 UDP `19830` 回归和桌面安装包

## 当前风险与技术债

- 当前全局 MQTT 凭证和无 TLS 配置不能用于公网；生产环境需使用 TLS、每设备凭证和最小权限 ACL。
- HTTP 上报可被伪造，缺少设备密钥或签名。
- 首个 Flyway 镜像 `v1.1.8-remote-mqtt` 已发布并完成群晖实卷基线、数据保留和升级后继续写入验证。
- 自动化回归已覆盖当前高风险链路，但仍需随核心功能修改持续补充。
- 前端主包体积较大，尚未进一步拆包。
- 腾讯云TCR国内同步源已发布`v1.1.9`前后端镜像并验证匿名解析和digest一致；GHCR继续作为主发布源。
- 群晖部署已完成首次验收，但仍需进行长时间稳定性、端口冲突覆盖和升级回归验证；详见 `synology-nas-deployment.md`。
- GHCR包应保持Public；`v1.0.0-lan`与远程`v1.1.0`至`v1.1.9`固定标签已发布。`v1.1.9`已经过漏洞门禁、签名来源证明和群晖升级验收，并已成功回退到1.1.8。

## 下一步

1. 决定是否进入AI日志分析阶段。
2. 若暂不开发新功能，继续观察`v1.1.9`长时间运行与每周镜像复扫。

持续维护：MQTT、状态流转、Flyway、前端轮询或Docker编排发生变化时，同步更新对应回归测试。

延期观察：本地 AI 回退官方 AI 偏慢、以及本地 AI 回答能力弱，均已记录但暂不处理；以后如决定优化，需在小智固件/本地 AI 服务项目执行。

详细任务见 `future-development-backlog.md`。
