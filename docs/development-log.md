# 开发记录

本文件记录关键里程碑和可复用的故障处理方法，按日期倒序排列。

完整早期过程保存在 `../history/docs-before-consolidation-2026-07-09.zip`，包括原始命令、长篇报错和逐步搭建过程。

## 2026-08-01 环境天气条件播报

问题：旧对话留下了环境空间、天气读取和 V10 表结构的半成品，但规则实际只按阈值立即投递，未实现连续确认、变化判断、静音、冷却或前端规则管理；动态 TTS 未配置时还可能回退固定测试语音，不能用于真实天气告警。

处理：规则评估现在记录每次命中或清除，使用 `CLEAR` 打断连续次数；温度/AQI 可按阈值或与上一读数的变化幅度命中，并经过连续、冷却和跨午夜静音判断。投递额外要求空间启用、主设备有效和动态 TTS 显式启用，其他情况记录 `SUPPRESSED`，发布异常记录 `FAILED`。环境监测页增加规则创建、查看与删除。

补充：实机页面显示天气服务已配置但空间未填写坐标，刷新按钮被正确禁用。原页面只能创建或删除空间，无法补填坐标；现增加编辑空间入口，复用既有 `PUT /api/environment-spaces/{id}`，不改变空间、规则或读数数据。

补充故障修复：真实刷新返回统一“室外天气服务请求失败”。核对和风 JWT 官方文档后，确认代码错误请求了 `/v7/grid-weather/now`；实时天气的正确端点是 `/v7/weather/now`，坐标参数为“经度,纬度”。空气质量接口 `/airquality/v1/current/{纬度}/{经度}` 保持不变。

验证：设置 `JAVA_HOME=C:\Program Files\Java\jdk-17` 后，后端 Maven 42 项测试通过（真实 MySQL 的 2 项条件测试跳过）；前端 TypeScript 检查和 Vite 生产构建通过。已构建、重新导入并校验私有离线包 `dist/aiot-remote-mqtt-v1.2.8-images.tar`，SHA-256 为 `f595c27fc5e2bb578ee0e8a80babf541f051027c709eb9985fb516156b3a9884`；5 套 Compose 部署回归通过。源码已提交并推送 `Remote-Hivemq` 的 `a2f9e74`。尚未进行群晖真实天气、动态 TTS 和小智实机投递验收。

## 2026-08-01 小智日志刷新延迟

问题：小智完成启动后，页面上的运行日志有时约 5 秒后才出现。日志页面原本以 1 秒轮询，并在请求尚未结束时直接跳过该轮；连续慢响应会累计跳过多个周期。

处理：日志刷新周期收紧为 500 毫秒。保留单请求互斥，避免重复查询；碰到未完成请求时，不再等待下一个完整周期，而是每 50 毫秒检查一次，请求结束后立即补发刷新。页面隐藏时仍停止请求。

验证：Vitest 覆盖“请求进行中不重叠、结束后 50 毫秒内恢复刷新”。用户决定等待环境监测功能完成后再统一构建离线镜像包；当前中间构建不作为部署包。届时以真实小智 MQTT 启动事件测量 MQTT 入库至页面显示的端到端时间。

## 2026-07-24 设备中心与远程 HiveMQ 页面凭据

问题：全局日志页把所有设备记录混在一起，设备详情只显示少量最近日志；自由文本设备类型无法稳定判断设备是否需要遥测图表。远程 HiveMQ 凭据只能修改部署环境文件，日常维护不方便。

处理：新增 Flyway V2 的 `monitoring_mode`，旧设备默认 `LOG_ONLY`，采集设备可选 `TELEMETRY`；设备列表和侧栏改为设备中心，设备工作台内固定按 `deviceId` 查询日志，并为采集设备展示数据表与趋势图。远程凭据增加只返回状态的 API、宿主机私有文件持久化和 Paho 立即重连；Broker 与密码继续隐藏。

验证：后端32项常规测试通过；另2项Flyway集成测试使用独立`mysql:8.4`容器中的MySQL 8.4.10真实执行，0失败、0跳过。空库依次执行V1和V2并到达版本2；旧库先保留V1设备、日志和传感器上报，再建立V1基线并执行V2，三类数据均保留且旧设备`monitoring_mode=LOG_ONLY`。临时数据库、容器和匿名卷均已清理，未连接项目或群晖数据卷。Flyway输出“当前版本已测试到MySQL 8.1”的升级建议，但MySQL 8.4两条实际迁移均成功。远程凭据新增2项私有文件保存/重载测试；前端4项测试、TypeScript检查和生产构建通过；5套Compose与镜像发布策略检查通过。尚未完成真实HiveMQ新凭据重连、镜像发布和群晖升级。

本地源码启动问题：远程源码Compose原本与群晖成品Compose都固定使用`aiot-log-backend-remote`。两者同时连接同一HiveMQ时，Broker按MQTT Client ID唯一性持续踢掉旧连接，表现为约数秒一次的`connection lost → reconnected`循环。立即停止本地前后端后，将仅用于本地源码构建的`docker-compose.remote.yml`改为`aiot-log-backend-remote-local-source`；GHCR/TCR群晖Compose继续使用原Client ID。重新创建后本地MQTT保持已连接、无最近错误，前端和后端正常。结论：同一HiveMQ下并行运行的后端实例必须使用不同Client ID。

报警等级调整与发布决定：`local_ai_discovery_failed`和`local_ai_fallback_to_official`表示本地AI不可用后已经正常使用官方AI，不属于需要处理的故障。后端现在无条件把这两类事件规范为`INFO`等级、`RUNNING`类型和`RESOLVED`状态，避免固件携带的`WARN/ERROR`造成误报警；真实`local_ai_connection_failed`以及官方AI连接或协议错误不降级。对应服务测试已覆盖固件上报较高等级时的规范化结果。该轮设备中心、Flyway V2、页面远程凭据和等级调整统一作为`v1.2.0-remote-mqtt`发布。

发布结果（2026-07-25）：标签推送触发的GHCR前端、后端构建均成功，后端候选镜像通过漏洞门禁后才推送，并为实际digest生成来源证明；GitHub Release已创建。随后`Remote-Hivemq`分支回归、已发布镜像复扫、分支构建和腾讯云TCR同步全部成功。TCR前后端均由GHCR固定标签复制，未重新构建，并通过源与目标digest一致性校验。群晖当前仍为1.1.9，待用户按原项目原地升级到1.2.0。

群晖验收结果（2026-07-25）：用户确认已原地升级`v1.2.0-remote-mqtt`且所有适用项目正常：原有数据保留，设备中心内独立日志自动刷新，远程 HiveMQ 页面保存用户名和密码后立即重连，重启后端后凭据继续生效。当前未接入采集型设备，故数据表和趋势图不虚构为已实机验证。前三阶段至此完成，下一阶段才是AI日志与遥测分析。

## 2026-07-23

### 前两阶段记录完整性复核

- 第一阶段范围：OpenAPI/Swagger、统一错误码、追踪号和结构化异常日志。完成事实分别记录在本文件下方两个专题和 `project-status.md`，选型与边界记录在 `technical-decisions.md` 的 0.8、0.9。
- 第一阶段问题记录：OpenAPI属于能力建设，没有线上故障；Spring Boot/Springdoc版本兼容、Swagger可信网络边界作为决策记录保存。错误治理明确记录了裸数字错误、HTTP状态不一致、缺少追踪号和日志字段不统一的问题、原因、处理与验证。
- 第二阶段范围：MQTT、状态流转、Flyway、前端轮询和Docker升级保护的自动化回归。完成事实记录在本文件“第二阶段自动化回归基础”和 `project-status.md`，分层理由与边界记录在 `technical-decisions.md` 0.10。
- 第二阶段问题记录：真实端点测试最初用一个同时缺少多个字段的请求断言固定错误消息，但Bean Validation不保证多个错误的返回顺序；测试数据改为只缺少一个目标字段，使断言稳定。开发机没有独立测试MySQL，因此2项Flyway测试采用条件启用，并由GitHub MySQL 8.4服务执行；不是跳过数据库验证。MQTT测试同时发现并补上Topic设备编号与Payload设备编号一致性校验。
- 结论：前两阶段均具备完成记录、决策记录和问题/边界记录；没有实际故障的建设项不虚构问题，明确标记为“无线上故障、记录选型与风险边界”。

### 第三阶段镜像发布质量

- 基础镜像：后端构建升级到 Maven 3.9.16 + JDK 17 Noble，运行时固定JRE 17 Noble维护线；前端构建升级到Node 24 Alpine 3.24，运行时升级到Nginx 1.30 Alpine。
- 发布门禁：GHCR发布工作流先构建本地候选镜像，使用Anchore Grype扫描，再推送同源缓存构建；存在“已有修复版本的CRITICAL漏洞”时阻断发布。扫描Action及Docker/GitHub Actions均固定到完整提交摘要，降低可变标签供应链风险。
- 来源证明：镜像推送后按实际digest调用GitHub官方`actions/attest`生成Sigstore签名的SLSA来源证明并写入GHCR，不维护长期私钥。签名绑定digest而非可变标签。
- 持续检查：新增每周和手工触发的已发布镜像扫描；默认从远程GHCR Compose解析当前固定标签，也可手工指定历史固定标签。
- 回归保护：新增`tools/test-image-release-policy.ps1`，检查扫描阈值、scan→push→attest顺序、完整Action摘要、签名绑定push digest、`latest`仅由`main`更新、远程前后端固定同一语义版本及维护中的基础镜像线。
- 初始实现状态：当时本地策略检查、4套Compose部署检查和3份工作流YAML解析通过；真实镜像构建、漏洞扫描和签名随后由GitHub Actions完成验证，最终结果见下方问题与发布记录。

#### 腾讯云TCR国内同步源

- 需求：在GHCR继续作为主发布源的前提下，为群晖增加国内镜像拉取地址并部署`v1.1.9-remote-mqtt`。
- 地址规范：用户提供的项目名包含大写和Markdown链接，不能直接作为镜像路径；按腾讯云个人版小写命名规则规范为`ccr.ccs.tencentyun.com/aiot-log-system/{aiot-log-backend,aiot-log-frontend}`。命名空间必须由腾讯云账号先创建且全局唯一。
- 实现：新增`sync-tcr.yml`，从已完成扫描和证明的GHCR固定标签复制OCI索引到TCR，不重新构建；禁止`latest`，并强制比较源、目标顶层digest。新增独立`docker-compose.remote.tcr.yml`和`-Remote -TencentRegistry`群晖部署包生成选项。
- 凭证边界：GitHub仓库当前没有Actions变量或Secrets；实际同步前必须配置`TCR_USERNAME`和`TCR_PASSWORD`，不得提交到代码、Compose或部署包。
- 发布验证：提交`bc7a099`触发同步运行`30001574691`，标签解析、腾讯云登录、前后端复制和digest校验全部成功；同提交回归运行`30001574743`通过。未登录客户端可解析两个TCR公有镜像，前端和后端顶层digest均与GHCR一致，并保留OCI索引中的attestation manifest。
- 群晖验收：使用TCR固定镜像原地重新构建`v1.1.9`，复用原MySQL数据卷、HiveMQ私有配置和端口。前端、后端、Swagger/OpenAPI均返回200，远程MQTT已连接，原46条日志保留；测试消息计数`2/2/0`，新增ID47 `RESOLVED`，总数47。升级成功，回退演练另行执行。
- 回退验收（2026-07-24）：前后端改回GHCR`v1.1.8-remote-mqtt`并原地重建，继续复用V1数据卷和私有配置。前端、运行时、MQTT与日志恢复，47条数据保留；Swagger/OpenAPI不再提供文档且新版Trace ID消失，符合旧版边界。测试MQTT`1/1/0`、新增ID48 `RESOLVED`、总数48，回退和继续写入均成功。
- 最终升回（2026-07-24）：前后端重新使用腾讯云TCR`v1.1.9-remote-mqtt`，48条数据保留；Swagger/OpenAPI、Trace ID和远程MQTT恢复。测试MQTT`2/2/0`、新增ID49 `RESOLVED`、总数49。最终运行版本确定为1.1.9。

#### 首次后端候选镜像被漏洞门禁阻断

- 问题：第三阶段首次GitHub实际运行中，前端镜像完成扫描、推送和来源证明，后端候选镜像在漏洞门禁停止，未推送该候选镜像。
- 根因：Spring Boot 3.3.5依赖管理带入`tomcat-embed-core 10.1.31`；Grype识别到多个已有上游修复版本的CRITICAL漏洞，其中修复版本达到Tomcat 10.1.55。失败发生在scan步骤，后续push和attest均被跳过，证明门禁顺序生效。
- 处理：保留现有Spring Boot 3.3.5兼容基线，显式把同一Servlet 6.0兼容线的Tomcat升级到当前10.1.57。Apache官方安全页确认10.1.x应通过升级发布版本取得安全修复；该方式比直接跨Spring Boot版本线改造影响更小。
- 本地验证：Tomcat 10.1.57已被实际加载；Maven发现32项测试，30项通过、2项因本地无MySQL条件跳过，生产JAR构建成功。发布策略检查增加Tomcat安全版本约束。
- 线上验证要求：重新推送后，后端候选镜像必须通过同一CRITICAL门禁并完成digest来源证明。若仍有CRITICAL项，不允许降低扫描阈值或添加忽略规则。

#### 已发布镜像复扫触发边界

- 问题：`image-security.yml`首次只提交在`Remote-Hivemq`，通过API手工触发时返回404。
- 根因：GitHub的`workflow_dispatch`和`schedule`只识别默认分支上已存在的工作流文件；新文件尚未进入默认分支，因此不能从当前分支直接手工调度，每周计划也不会自动运行。
- 处理：增加仅在`Remote-Hivemq`修改该工作流或远程GHCR Compose时触发的push入口，用于立即验证当前固定镜像复扫；手工指定历史标签和每周计划保留，待工作流进入默认分支后启用。
- 验证：提交`1803e26`触发分支push复扫，标签解析、`v1.1.9-remote-mqtt`后端扫描和前端扫描全部成功，GitHub Actions运行`29994470052`通过；同提交的回归运行`29994469929`也通过。
- 边界：不为启用计划任务而擅自把远程业务代码合并进`main`。每周和手工入口仍需该工作流进入默认分支后才会正式生效。

### OpenAPI/Swagger 接口文档

- 处理：后端接入与 Spring Boot 3.3.x 兼容的 Springdoc 2.6.0，新增中文 OpenAPI 元数据和 `aiot-api` 分组，仅收录 `/api/**`。9 个业务控制器补充中文 Tag 与 Operation，通用响应和分页结构补充 Schema；根路径和 Actuator 不进入业务文档。
- 使用：当前源码提供 `/v3/api-docs/aiot-api` 和 `/swagger-ui.html`，可通过 `OPENAPI_ENABLED`、`SWAGGER_UI_ENABLED` 关闭。文档仅供可信网络调试，不改变现有安全边界。
- 验证：新增配置反射检查和随机端口真实端点测试，确认文档 JSON、Swagger UI 均返回 HTTP 200，且系统运行时、日志和 MQTT 状态等主要路径存在。JDK 17 Maven 共 19 项测试全部通过，生产 JAR 构建成功。
- 发布状态：已随 `v1.1.9-remote-mqtt` 发布并在群晖完成Swagger/OpenAPI真实端点验收。

### 统一错误码、异常响应与请求日志

- 问题：业务代码直接抛出裸 `400/404/409/500`，字符串消息既承担程序判断又承担用户显示；全局处理器没有设置真实 HTTP 状态，也缺少请求关联标识。
- 处理：增加集中 `ErrorCode` 和类型化 `BusinessException`；响应保留数值 `code`，新增稳定 `errorCode` 和 `traceId`，并让 HTTP 状态与响应一致。请求过滤器生成或沿用合法 `X-Trace-Id`，写入响应头和 MDC。全局处理器统一覆盖校验、参数类型、JSON、方法、路由、数据冲突和未知异常。
- 日志：可预期拒绝使用 `api_request_rejected`，内部业务操作失败使用 `api_operation_failed`，未知异常使用 `api_unhandled_error`；字段采用 `event/errorCode/status/method/path/traceId/detail`，不记录请求体、密码、Token 或 MQTT 凭证。前端错误对象同步保留 `errorCode` 和 `traceId`。
- 验证：新增4项随机端口真实 HTTP 测试，覆盖业务错误、字段校验、参数格式、损坏 JSON、404路由及客户端追踪号透传；后端共23项测试全部通过，后端生产 JAR、前端类型检查和 Vite 生产构建成功。相关功能已随`v1.1.9-remote-mqtt`发布。

### 第二阶段自动化回归基础

- MQTT与状态流转：新增订阅器单元测试，覆盖`/log`、`/report`路由、QoS、计数、非法JSON、字段缺失、Topic与Payload设备编号不一致、断线/恢复回调；新增QoS 1重复故障不追加和非MQTT恢复事件不误关故障测试。订阅器拒绝日志改为只记录Topic、Payload字节数和原因，不输出原始Payload。
- Flyway：新增真实MySQL条件集成测试。空库必须执行V1并创建7张业务表；旧库先执行冻结V1并插入标记设备，Flyway接入后必须生成`BASELINE`且数据仍存在。本地没有测试MySQL时跳过，GitHub工作流使用MySQL 8.4完整执行。
- 前端：使用Vitest 4.1.10，将日志页和MQTT状态页的定时逻辑抽为共享控制器；4项测试覆盖可见时每秒刷新、隐藏暂停、恢复立即刷新、请求进行中跳过、恢复后继续以及重复恢复不产生多个定时器。
- Docker：新增部署回归脚本，当前调用`docker compose config`验证5套编排，并检查远程模式不启用Mosquitto/UDP发现、GHCR/TCR前后端标签一致且非`latest`、MySQL命名卷持续复用、4个停止脚本不使用删卷命令。
- 自动执行：新增GitHub `regression.yml`，在每次相关推送或PR中运行MySQL 8.4后端测试、前端测试与构建、部署检查。本机已验证后端32项发现（30通过、2项因无MySQL跳过）、前端4项通过、生产构建和部署检查通过。

## 2026-07-22

### Flyway 无损数据库迁移基线

- 问题：MySQL 命名卷可以保留现有数据，但新镜像如果增加字段、索引或表，原来的 `docker-entrypoint-initdb.d/schema.sql` 不会在已有数据卷上再次执行，数据库结构无法随镜像自动升级。
- 处理：后端加入 `flyway-core` 与 MySQL 支持模块，新增 `V1__create_initial_schema.sql`；配置 `baseline-on-migrate=true` 和基线版本 1。空库执行 V1，已有完整数据库只增加 `flyway_schema_history` 基线记录。四套 Compose 显式创建 `aiot_log_system` 数据库；`sql/schema.sql` 冻结为 V1 兼容引导，后续结构变化只能新增 Flyway V2、V3 迁移。群晖部署包继续兼容当前已发布的 pre-Flyway 镜像。
- 验证：JDK 17 Maven 15 项测试全部通过，生产 JAR 构建成功。使用隔离的 MySQL 8.4 临时实例分别验证空库和旧库：空库创建 7 张业务表并记录 `V1/SQL/success`；旧库预置测试设备后启动新后端，测试数据保留且只记录 `V1/BASELINE/success`。四套 Compose 配置、部署包生成脚本和两种群晖包均通过检查，临时数据库和文件已清理。
- 发布与群晖验收：Flyway 实现已作为 `v1.1.8-remote-mqtt` 固定标签发布，前后端 GHCR 镜像和 GitHub Release 均由标签工作流生成；远程 GHCR Compose 已固定引用该版本。群晖在备份后复用原数据卷和私有配置完成原地升级，启动日志出现 `JdbcTableSchemaHistory` 与 `DbBaseline` 成功记录，原45条日志和设备数据保留。升级后 MQTT 测试收到/处理/失败为 `1/1/0`，新增 ID 46，日志总数变为46，确认旧库基线与后续写入均正常。

## 2026-07-21

### 群晖长时间运行与后端停止/启动验收

- 验证：后端已连续运行约 20 小时。正常重启后，API 首次检查即恢复，远程 MQTT TLS 自动重连，进程内 MQTT 计数从零开始，系统运行时长重新计时，原 40 条日志和 MySQL 数据保持不变。
- 停止/启动：手动停止后，API 连续请求超时，前端静态日志页仍可访问并保留已有列表，同时显示请求失败提示；重新启动后，API、MQTT、日志页每秒轮询和运行时长均自行恢复，无需切换菜单。
- 发现问题：顶部“后端已接入”是 `AppHeader.vue` 中的固定标签，API 不可达时仍显示在线，属于状态误报。运行时长恢复正常；窄窗口下看不到它是 `max-width: 720px` 样式主动隐藏，不是恢复缺陷。
- 处理：顶部状态改用轻量 `/api/system/runtime` 探测；初始、在线、离线分别显示“后端检测中”“后端已接入”“后端未连接”。请求失败时清空过期运行时长，每 5 秒及页面恢复时自动重试。
- 验证：前端 TypeScript 检查和 Vite 生产构建通过。修复尚未发布镜像或部署群晖，仍需实机停止/启动复测。

### MQTT 故障恢复跨汇总窗口后遗留待处理记录

- 验证：通过 MQTTX 向单一远程订阅实例依次发送连接失败、连接恢复、再次失败事件；后端收到/处理成功/失败计数为 `3/3/0`，三条新记录依次为 `PENDING`、`RESOLVED`、`PENDING`，中文映射、QoS 1 接收和页面刷新均正常。
- 问题：第一次失败与恢复相隔约 75 秒，恢复记录虽然为 `RESOLVED`，但原失败记录仍为 `PENDING`，因此并未真正关闭同一故障。
- 根因：`LogServiceImpl.findRecentRuntimeLog` 只查找 `updatedAt >= now - 30 秒` 的运行日志；恢复事件只有合并进该窗口内的既有记录时才会更新原状态，超出窗口便新建记录。现有单元测试只覆盖同一窗口内的状态流转。
- 处理：把“30 秒事件汇总”与“未恢复故障生命周期”分离。恢复事件会先查找同设备最新的未恢复 MQTT 连接故障并置为 `RESOLVED`，再按原窗口决定追加或新建恢复日志；两步纳入同一事务，写入失败时不会只关闭旧故障。
- 验证：新增跨窗口恢复测试；JDK 17 Maven 共 15 项测试、0 失败，原同窗口状态流转、QoS 1 相邻去重、中文映射和运行时长测试均继续通过。群晖实测仍待新镜像部署；本轮此前生成的三条测试日志保留，未自动删除数据库数据。
- 发布：提交 `7660f48` 已推送到 `Remote-Hivemq`；标签、GitHub Release 和前后端 GHCR `v1.1.7-remote-mqtt` 镜像均已发布，两个公开镜像清单验证可解析。旧标签全部保留，`latest` 继续只代表 `main` 局域网版。

## 2026-07-20

### 后端容器缺少标准就绪判断

- 问题：Compose 只等待 MySQL 健康，前端只等待后端容器进入启动状态；Spring Boot 尚未真正可用时，前端仍可能先启动，容器管理界面也无法区分“进程已启动”和“服务已就绪”。
- 处理：增加 Spring Boot Actuator，健康端口默认固定为容器/本机回环地址 `127.0.0.1:8081`，仅暴露不含组件详情的 `health`；这些应用默认值也覆盖仍使用旧私有配置文件的本地启动场景。Docker 镜像加入 `curl`，源码构建的局域网和远程 Compose 用内部健康检查判定后端，并让前端等待 `service_healthy`。
- 安全边界：不映射 `8081`，不增加公网端口，不显示数据库、MQTT、主机或凭证细节；实现和发布阶段未自动修改既有群晖部署。
- 当前验证：远程前后端镜像构建通过；使用 Maven 17 容器执行 14 项后端测试，结果全部通过。隔离启动临时 MySQL 和禁用 MQTT 的候选后端后，容器内 `GET /actuator/health` 返回 `UP`，同一 Docker 网络中的其他容器无法访问 `8081`，确认管理端口仅限回环地址。POM XML、三个相关 Compose 结构、敏感信息扫描、Markdown 本地链接检查和 `git diff --check` 也通过；临时容器、网络和无持久卷测试数据已清理。
- 发布：提交 `c853d6c` 已推送到 `Remote-Hivemq`，独立标签、GitHub Release 以及前后端 GHCR `v1.1.6-remote-mqtt` 镜像均已发布并验证清单可解析；远程 GHCR Compose 已固定引用该版本。
- 群晖升级：在原 `aiot-log-system-remote` 项目中复用 MySQL 数据卷、私有环境文件和既有宿主机端口映射完成升级，没有执行卷删除；前端、后端和 MySQL 均正常运行，后端容器显示 `healthy`，证明容器内部 Actuator 健康检查已生效。

### 长时间运行后日志页没有继续刷新

- 现象：群晖运行两天后，设备启动日志已进入后端，但日志页不显示；进入 MQTT 状态页再返回后积压日志出现，之后设备再次重启即可实时显示。
- 判断：数据能在页面重新挂载后立即出现，证明小智、HiveMQ、后端订阅和数据库入库链路正常，故障在前端轮询生命周期。
- 根因：日志页等待 `loadOptions()` 和首次 `loadData()` 成功后才创建定时器；任何初始请求失败都会终止挂载钩子，使轮询和恢复监听均未安装。
- 处理：定时器及可见性、聚焦、联网和 `pageshow` 监听改为先建立；初始请求独立执行，失败不终止轮询；页面恢复时主动重建定时器并立即刷新，保留 1 秒间隔、请求互斥、筛选和分页。

### 系统运行时长显示

- 后端新增轻量只读运行时接口，使用 JVM 启动时间计算后端进程运行秒数；不访问数据库、不返回主机或凭证信息。
- 管理页面顶部新增“系统运行时长”，显示“天 + 时:分:秒”，每秒本地递增、每分钟校准，页面或网络恢复时立即同步；后端重启后重新计时。
- 验证：JDK 17 Maven 测试 14 项全部通过，前端 TypeScript 检查和 Vite 生产构建通过；敏感信息扫描、29 个 Markdown 文件的本地链接检查和 `git diff --check` 全部通过。
- 发布：提交 `d0e6d4f` 已推送到 `Remote-Hivemq`，独立标签、GitHub Release 以及前后端 GHCR `v1.1.5-remote-mqtt` 镜像均已发布并验证可解析。
- 群晖验收：原远程项目已升级前后端到 `v1.1.5-remote-mqtt` 并复用原 MySQL 数据卷和私有环境文件。浏览器强制刷新后确认系统运行时长逐秒增加、MQTT 保持已连接；停留在日志管理页重启小智时，新日志无需切换 MQTT 状态页即可约 1 秒自动出现，浏览器标签隐藏后返回也会立即补刷。

## 2026-07-17

### v1.1.4 远程版发布与群晖升级

- 发布：提交 `9ee4379` 已推送到 `Remote-Hivemq`，创建独立标签 `v1.1.4-remote-mqtt`；GitHub Actions 的后端镜像、前端镜像和 Release 任务全部成功，GHCR 镜像清单可正常解析。
- 版本隔离：旧远程标签 `v1.1.3-remote-mqtt`、`v1.1.2-remote-mqtt`、`v1.1.1-remote-mqtt`、`v1.1.0-remote-mqtt` 和局域网 `v1.0.0-lan` 均保留；远程标签没有覆盖局域网语义的 `latest`。
- 群晖：已成功拉取 `v1.1.4-remote-mqtt` 前后端镜像并升级现有远程项目，继续复用原 MySQL 数据卷和私有 HiveMQ 环境文件，没有开放家庭 MQTT 或数据库端口到公网。
- 待验收：浏览器强制刷新后，使用真机确认详情弹窗每秒同步新增事件、当前记录不在列表页时详情接口兜底，以及本地 AI 未发现/回退官方 AI 的中文显示。

### 日志详情弹窗未跟随列表实时更新

- 问题：日志列表每秒轮询后，同一条运行汇总日志已经追加新事件，但打开中的详情弹窗仍显示点击行时保存的旧对象快照。
- 根因：`currentLog` 只在点击详情时赋值，列表刷新只替换 `logs`，没有同步当前详情；记录离开当前筛选页时也没有单独获取详情。
- 处理：列表返回同 ID 时原位更新 `currentLog`；不在当前页时通过带防缓存参数的详情接口静默获取。详情请求增加互斥、排队和版本校验，防止重复并发及旧响应覆盖；业务码 404 时安全关闭弹窗，普通网络失败不影响列表轮询。
- 验证：前端 TypeScript 检查和 Vite 生产构建通过；镜像已发布并部署群晖，仍需在浏览器中验证同页更新、跨页详情兜底、隐藏标签暂停和返回立即刷新。

### 本地 AI 未发现与回退官方 AI 仍显示英文

- 问题：固件上报 `local_ai_discovery_failed`、`local_ai_fallback_to_official` 及对应英文消息时，IoT 页面缺少中文标题和内容。
- 根因：现有映射只覆盖本地 AI 发现成功、连接成功、连接失败和 WebSocket 握手，没有覆盖“本次启动未发现”与“已正常回退官方 AI”。
- 处理：后端 `resolveDeviceRuntimeContent`、`localizeRuntimeEvent` 和前端历史兼容映射均补充两类事件；不把它们纳入日志 MQTT 故障或恢复判断，也不修改官方 AI 协议。
- 验证：JDK 17 Maven 测试 12 项全部通过，包含新增标题/内容映射及原有远程 MQTT、`WARN`、去重、启动批次和状态流转回归；已作为独立版本 `v1.1.4-remote-mqtt` 发布并部署群晖。

## 2026-07-15

### 群晖 HiveMQ 远程版实机部署验收

- 验证：使用固定 GHCR 标签 `v1.1.0-remote-mqtt` 在 DSM Container Manager 创建独立远程项目；MySQL 健康，后端和前端启动，MQTTX 测试消息经 HiveMQ TLS 到达群晖后端并处理成功。
- 问题：群晖 Docker CLI 不支持 `docker compose` V2 子命令；既有 NAS 服务占用宿主机 `8080`，后端首次启动时报 external connectivity。
- 根因：DSM 通过 Container Manager 管理 Compose，而当前 Docker CLI 没有 Compose V2 插件；端口冲突只发生在宿主机映射，容器内部服务地址没有冲突。
- 处理：使用 Container Manager 从部署目录创建项目；将远程后端宿主机端口改为 `18080`、前端改为 `18000`，保留容器内部 `backend:8080`。
- 边界：原局域网项目和数据未删除，但两套项目不同时启动；未记录真实 NAS 地址、HiveMQ 域名或凭证，也未开放家庭 MQTT、数据库或后端端口到公网。

### GHCR 局域网版与远程版标签隔离

- 问题：发布 `v1.1.0-remote-mqtt` 时，GitHub Actions 同时把 `latest` 指向了远程镜像，导致页面无法直接通过稳定名称识别原局域网镜像。
- 根因：工作流使用元数据动作的 `{{is_default_branch}}` 判断；版本标签事件没有按预期排除 `latest`。
- 处理：将 `latest` 的生成条件改为明确匹配 `refs/heads/main`；原局域网镜像增加固定标签 `v1.0.0-lan` 并恢复 `latest`，远程镜像继续使用 `v1.1.0-remote-mqtt`。
- 验证：两个 GHCR 包均已同时提供 `v1.0.0-lan` 和 `v1.1.0-remote-mqtt`；前后端的 `latest` 与 `v1.0.0-lan` 镜像摘要分别完全一致，远程版摘要不同且仍保留。工作流修复分支运行与局域网标签发布运行均成功。

### HiveMQ 远程 Docker 端到端验收

- 验证：Windows Docker 远程 Compose 已启动 MySQL、后端与前端；后端以 `MQTT_MODE=remote` 成功通过 TLS `8883` 订阅 HiveMQ，MQTTX 发布的测试日志已成功入库。
- 问题：初次处理失败分别显示设备不存在、日志类型不合法。
- 根因：测试 Topic、Payload 的 `deviceCode` 与管理端已创建的设备编号不一致；`SYSTEM` 不在后端 `logType` 枚举内。
- 处理：统一使用同一设备编号；将 `logType` 改为允许值 `RUNNING`、`ERROR`、`MAINTENANCE` 或 `INSPECTION`。
- 验证：使用 `RUNNING` 重新发布后，MQTT 状态页的处理成功计数增加。真实 HiveMQ 域名、用户名、密码与 Token 未写入文档或仓库。

### Windows WSL、Docker Hub 与截图工具故障

- 问题：异常重启后 Docker Desktop 停留在 “Starting the Docker Engine”，WSL 报 `system.vhd` 挂载的 `HCS/ERROR_NOT_SUPPORTED`；Docker Hub 匿名 Token 请求偶发超时；Windows 截图工具选区后卡死。
- 处理：以管理员身份执行 `bcdedit /set hypervisorlaunchtype auto` 后重启，使用 `wsl -d Ubuntu-22.04 -- echo WSL_OK` 确认 WSL 恢复后再启动 Docker。Docker Hub 超时时先预拉取构建基础镜像；本机 localhost 代理场景使用 `%UserProfile%\.wslconfig` 的镜像网络与 `autoProxy=true`，随后 `wsl --shutdown` 并重启 Docker Desktop。
- 验证：WSL 返回 `WSL_OK`、Docker Engine 恢复，基础镜像拉取及远程 Compose 构建启动成功。
- 边界：上述均为 Windows 主机环境问题，不修改 AIoT 项目业务代码；不要删除/注销 WSL 发行版、`system.vhd`、Docker 数据卷或私有远程配置。截图工具问题应作为 Windows/显卡驱动问题单独处理。

## 2026-07-14

### 群晖 NAS 成品镜像实机部署验收

- 已在 DSM Container Manager 通过 GHCR 前端、后端成品镜像完成部署；MySQL、Mosquitto 因 Docker Hub TLS 握手超时和 EOF 改由本地导出后导入 NAS。
- 已处理 NAS 现有服务造成的网页/后端端口冲突，使用私有 `.env` 调整宿主机端口，不改变容器间服务名通信。
- 已修复 Mosquitto 绑定密码文件/ACL 的 UID/GID `1883` 读取权限，四容器启动正常。
- 已将发现 Token 改为空默认值并重新创建后端配置；设备已通过 UDP `19830` 自动发现 Broker 并连接成功。
- 详细可复用流程、限制和排障见 `synology-nas-deployment.md`；未记录真实网络标识或凭证。

## 2026-07-13

### Docker 新用户空白数据库

- 问题：新用户首次创建 Docker 数据卷时，Compose 会自动挂载 `sql/init-data.sql`，导致管理页面出现演示设备、日志和标签，不符合正式新用户应从空白数据开始的要求。
- 处理：源码构建和 GHCR 成品镜像 Compose 均只挂载 `sql/schema.sql`；群晖镜像部署包生成脚本不再复制演示 SQL。`init-data.sql` 保留供本地开发演示使用，但不自动执行。
- 影响：已有命名卷的数据不会被修改；希望清空已有演示数据时，需由部署者明确删除数据卷后再创建项目。

### Docker UDP 自动发现默认配置

- 问题：Docker 初始化脚本自动生成发现 Token，但常规设备固件没有该 Token，服务端会以 `token mismatch` 拒绝 UDP `19830` 自动发现；手动填写 Broker 地址仍可连接。
- 处理：发现 Token 改为默认留空，Compose 允许空值；设备自动发现 Broker 地址后仍需使用已配置的 MQTT 账号密码连接。只有设备端也预置同一 Token 时才允许部署者手动在私有 `.env` 启用。

### 群晖 DSM / Container Manager 初始化

- 问题：Container Manager 只读取 Compose，不会执行 Windows 的 `docker-update.cmd` 和 PowerShell 初始化脚本；仅上传 Compose 后会依次缺少 MySQL 初始化 SQL 和 `docker/local/` 私有 Mosquitto 挂载文件。
- 处理：新增 POSIX `tools/initialize-synology-docker-config.sh`，通过 DSM SSH 在项目根目录运行一次即可生成并复用私有 `.env`、Mosquitto 配置/密码哈希/ACL、后端 MQTT 凭证和局域网发现地址；README 与部署设计补充完整源码上传和执行顺序。
- 补充：新增 `tools/create-synology-image-deploy.ps1`，将 GHCR Compose、两份 SQL 和 NAS 初始化脚本整理为无前后端源码的部署包；NAS 已拉取 `aiot-log-backend`、`aiot-log-frontend` 时应使用此包而不是源码构建 Compose。
- 修正：DSM 绑定挂载中，`mosquitto-passwords` 若仅归 root 所有，Mosquitto 容器内的 UID/GID `1883` 无法读取而持续退出；群晖初始化脚本改为将密码哈希和 ACL 均设为 UID/GID `1883` 所有、权限 `0600`。
- 修正：Docker 后端从 MQTT 状态页保存新全局凭证时会重写密码哈希和 ACL；现会在 Docker 配置目录中立即恢复 UID/GID `1883` 与 `0600`，避免下次重启 Broker 无法读取更新后的凭证。
- 验证：脚本逻辑已完成静态检查；尚待目标群晖完成首次端到端构建验收。

### GHCR 成品镜像发布与部署入口

- 新增 GitHub Actions 工作流：向 `main` 推送、推送 `v*` 版本标签或手动触发时，构建前端和后端镜像并推送至 GHCR。
- 新增 `docker-compose.ghcr.yml` 与 `docker-ghcr-start.cmd`、`docker-ghcr-update.cmd`、`docker-ghcr-stop.cmd`；使用者仅拉取成品镜像，不再本地构建 Java/Vue 源码。
- GHCR 部署继续使用现有私有 `.env` 和 `docker/local/` 自动生成机制，镜像不携带真实数据库密码、MQTT 凭证、Token、MAC/IP 或运行数据。
- GitHub CLI 未安装，未直接操作远程 Package；首次推送工作流并成功构建后，需在 GitHub Packages 手动将两个镜像设为 Public，随后才能进行真实拉取验收。

### Docker 私有配置与 MQTT 安全更新

- Docker Compose 移除公开默认数据库密码和匿名 Mosquitto，改为要求私有环境变量，并挂载被忽略的 `docker/local/` 目录。
- 新增 `tools/initialize-docker-config.ps1`：首次运行 Docker 脚本时自动生成或复用本机 `.env`、MQTT 凭证、Mosquitto 密码/ACL、Spring Boot MQTT 属性和 UDP 发现地址；不读取、不打印真实值。
- Docker 后端安装 `mosquitto_passwd`，MQTT 状态页更新凭证时会同步写入仅本机使用的 Spring Boot 属性文件，重启相关容器后生效。
- Docker 启动/更新脚本均会执行初始化；切换 Wi-Fi、网线或热点后再次启动会刷新局域网发现地址。未启动服务；需在停止本地开发版后执行 `docker-update.cmd` 完成重新构建验收。

### GitHub 展示与交付材料整理

- 新增面向公开展示的根目录 README：说明项目范围、技术栈、两种启动方式、真实小智接入、MQTT 安全边界、实机验证和已知限制。
- 使用 `docs/architecture.mmd` 维护 Mermaid 架构图；根目录 README 引用同一结构，避免展示图和设计文档长期分叉。
- 新增 `screenshots/README.md`，明确首页、`XIAOZHI-001` 详情、中文运行日志、MQTT 认证状态、批量删除模式五类截图及逐项脱敏要求。
- 新增 `SECURITY-CHECKLIST.md`，并补充 `.gitignore`，禁止提交真实 MQTT 凭证、Mosquitto 密码文件、真实设备和网络标识、运行日志、数据库导出、私有截图及抓包。
- 未读取、输出或写入任何真实密码、Token、运行数据；截图仍由人工使用脱敏演示数据准备。

### 项目展示主体调整

- GitHub 展示统一以“通用 AIoT 日志管理系统”为主体；小智 ESP32-S3 明确定位为当前已完成实机验收的设备接入示例。
- README、架构图、项目上下文、状态和设计文档均保留小智验证成果，但不将其表述为系统唯一设备或项目主体；其他设备可通过通用 MQTT、HTTP 或协议适配器接入。

### 公开配置模板与本地配置隔离

- 新增后端、Mosquitto LAN、Mosquitto ACL 和 MQTT 凭证的公开模板文件；模板只保留配置结构与占位符，不包含真实账户、密码、Token、MAC、IP 或本机路径。
- 本机运行文件继续保留原位置供现有脚本使用，并通过 `.gitignore` 防止被后续暂存；该调整不改变本地服务行为。
- 发布前仍需检查 Git 历史中是否已有受跟踪的本地配置。忽略规则无法移除旧提交中的文件，首次公开推送前必须采用干净的公开历史。

### 小智本地 AI 与日志链路实机验收

- 本地 AI：设备经本机 OTA 接口获取 WebSocket 配置后，服务端收到 `hello`、`listen` 和 MCP 工具协商，并完成本地 TTS 回复，确认并非只完成发现。
- 日志：已验证普通 Wi-Fi、手机热点、断网恢复，以及 30 秒运行事件汇总。
- 修正：`Local AI server discovered` 仅显示“已发现本地 AI 服务”，不得被 IoT 端篡改为“已连接”；连接、失败和官方回退必须由固件上报明确事件。

### 全局 MQTT 凭证管理

- 决策：当前家庭/开发阶段采用一套全局设备 MQTT 用户名密码，所有设备在配网页填写同一套；设备留空或认证失败时只停止日志上传，不影响 AI 或启动。
- 实现：MQTT 状态页新增全局凭证注册/修改、认证状态与关闭匿名访问入口；凭证和 Mosquitto 密码文件不纳入版本控制。
- 流程修正：新建认证的标准顺序为“创建凭证 → 关闭匿名访问 → 重启服务 → 设备填写凭证”；先配置设备仅用于已有匿名设备的无中断迁移。

### 重启后的运行日志批次边界

- 问题：设备在 30 秒汇总窗口内复位时，上一轮末尾的 `Official AI protocol connected` 被追加到新一轮启动事件，形成 7 条混合记录。
- 修复：后端收到 `startup` 或 `firmware_started` 时强制新建“设备运行上报”日志，后续 Wi-Fi、日志 MQTT、AI 协议事件才继续追加到该新批次。
- 中文化：兼容 `Official AI protocol connected` 与 `Official AI protocol connected or reconnected`，分别显示为“官方 AI 协议已连接”“官方 AI 协议已连接或重连”。
- 中文化补充：`Local AI WebSocket hello completed` 显示为“本地 AI WebSocket 握手完成”。

### 本地启动等待优化

- 现象：`start-all.cmd` 显示“Starting backend...”后需要数秒才继续，容易被误认为卡住。
- 定位：后端本身启动约 2 秒；脚本原先每秒才检查一次端口，额外增加最多约 1 秒可避免等待。
- 修复：端口就绪轮询间隔由 1 秒改为 200 毫秒，保留完整就绪确认，避免启动过早打开网页。

### 日志列表自动刷新

- 问题：设备上报新日志后，日志管理页需要手动刷新浏览器才能看到。
- 修复：页面可见时每 5 秒刷新当前日志列表；浏览器标签页隐藏或离开页面时停止，返回时立即刷新。
- 约束：保留当前筛选条件、当前分页及弹窗状态，不会自动跳页或覆盖用户编辑内容。

### 日志勾选与批量删除

- 问题：日志数量增加后，逐条点击删除操作繁琐。
- 修复：日志列表新增行勾选、当前页全选和“删除选中（n）”入口；支持跨页保留选择。
- 安全：执行删除前必须二次确认；后端校验所有编号存在后，在同一事务中删除日志和关联标签，不提供一键清空全部日志。
- 交互修正：勾选列默认隐藏，点击“重置”右侧的“批量删除”后才显示；批量删除或取消时同时清空 Element Plus 表格的内部选择，修复第二次删除仍携带第一次已删除日志编号的问题。

### 本轮实机验收完成

- 已重启本地 IoT 服务并完成设备 RST 验证：新的启动事件不会再与上一轮日志混合，英文 AI 事件显示为中文。
- 已验证日志管理页自动刷新、批量删除和第二次选择计数，未再出现旧日志编号或“日志不存在”错误。
- 已创建全局 MQTT 凭证、关闭匿名访问并重启服务；小智填写相同凭证后仍成功上报日志。
- 本地 AI 取消/不可用时已能回退官方 AI，但用户实测回退偏慢；本地 AI 能力也弱于官方。该问题不由 IoT 后端修复，后续在小智固件/本地 AI 服务侧优化。

## 2026-07-12

### 网络切换、断网恢复与本地 AI 联调

- 手机热点：电脑和小智同连手机热点后，设备通过 UDP `19830` 自动发现热点下电脑的新局域网 IPv4，并继续向 Mosquitto TCP `1883` 上传日志。
- 断网恢复：设备网络临时断开后恢复，日志 MQTT 可重新发现/连接 Broker 并继续上传；本地日志失败不影响设备启动与 AI 通道。
- 本地 AI：设备收到本机 OTA 配置后建立 WebSocket 会话；本地服务收到 `hello`、`listen`、MCP 工具协商，并完成 TTS 唤醒回复，确认本地 AI 实际对话成功。
- 状态语义修正：发现 `Local AI server discovered` 不能直接等价为“已连接”。IoT 显示拆分为“已发现本地 AI 服务”“已连接本地 AI 服务”“本地 AI 服务连接失败”；连接与回退必须由固件明确上报。
- 已知问题：固件曾在 30 秒汇总窗口中发布两轮相同启动序列，形成“设备运行上报（8 条）”；后端按收到消息合并，待固件去除重复初始化/发布。

## 2026-07-11

### 真实小智日志接入与界面可读性

- 新增 MQTT `/log` Topic 消费：`aiot/device/+/log` 写入 `logs` 表并标记为 `DEVICE` 来源；保留 `/report` 写入设备上报数据。
- 实机验证：`XIAOZHI-001` 发布 `startup`、`wifi_connected`、`mqtt_connected`、官方 AI 协议状态等事件，后端 MQTT 计数确认接收和处理成功。
- 日志汇总：同一设备默认 30 秒内连续事件合并为一条“设备运行上报（n 条）”，详情按事件顺序展示，避免一次启动产生多条列表记录。
- 中文化：已知固件英文事件和消息在后端新入库、前端历史兼容显示中统一为中文；标题列改为单行省略和悬浮提示。
- 时间处理：启动早期设备尚未同步时间会产生错误时间；运行日志改由后端接收时间记录，避免显示 `1970-01-01`。

## 2026-07-10

### 小智独立日志通道与局域网 Broker 方案

- 架构确认：小智日志 MQTT 与 AI 通道解耦；日志异常、重连和发送失败不得阻塞 Wi-Fi、语音、音频、唤醒或 AI 对话。
- 局域网 Mosquitto：新增本地 LAN 配置，目标监听 `0.0.0.0:1883`；Windows 防火墙仅允许专用网络本地子网访问 TCP `1883`。
- 自动发现：IoT 端设计并实现 UDP `19830` Broker 发现响应，校验协议、nonce 和可选 Token，向设备单播当前可访问的私有 IPv4、端口和 TLS 信息。
- 设备接入方式：固件优先自动发现 Broker，失败时使用手动地址兜底；更换普通 Wi-Fi、网线或手机热点无需重新编译固件。

## 2026-07-09

### 文档结构整理

- 事项：原文档数量多，状态、计划和开发记录存在重复。
- 处理：新增根目录 `AGENTS.md`、`PROJECT_CONTEXT.md`；当前文档收敛为 6 份，合并前原文统一压缩保存到 `history/`。
- 结果：新对话只需先读取 `PROJECT_CONTEXT.md`，专题资料按 `docs/README.md` 查找。

### Docker 启动页面出现 502

- 问题：Nginx 已启动，但 Spring Boot 尚未就绪，浏览器过早打开导致 `/api` 返回 `502`。
- 处理：`docker-start.cmd` 先轮询后端统计接口，再检查前端，最后打开浏览器。
- 结果：启动页面不再因后端未就绪产生初始 502。

### Docker 初始化中文乱码

- 问题：页面固定文字正常，但 Docker 初始设备、标签和日志数据乱码。
- 根因：SQL 中文在首次导入时按错误客户端编码解释后存入。
- 处理：
  - 使用可逆字符集转换原地修复当前数据。
  - `schema.sql`、`init-data.sql` 增加 `SET NAMES utf8mb4`。
  - JDBC 显式使用 UTF-8，Servlet 响应强制 UTF-8。
- 验证：MySQL 查询和 HTTP 原始 UTF-8 响应均显示正确中文。

### Docker Compose 完整验收

- 服务：frontend、backend、mysql、mosquitto。
- 验证：MySQL 为 `healthy`；网页与后端接口返回正常；MQTT 状态为已连接。
- 脚本：
  - `docker-start.cmd`：日常快速启动。
  - `docker-update.cmd`：首次部署或代码更新后重建。
  - `docker-stop.cmd`：停止并保留数据。

### Docker Maven 依赖下载中断

- 问题：容器内访问 Maven Central 时出现 `Premature end of Content-Length`。
- 根因：国外仓库网络传输中断。
- 处理：
  - 新增 `backend/maven-settings-docker.xml`，使用国内 Maven 公共镜像。
  - Dockerfile 增加 Maven 下载重试。
  - 使用 BuildKit 缓存 `/root/.m2/repository`。
- 结果：后端镜像构建成功，重试时可复用已下载依赖。

### Docker Hub 镜像下载不稳定

- 问题：出现 TLS 握手超时、`unexpected EOF`、OAuth token 获取失败。
- 根因：Docker Desktop、WSL 2 与 VPN 代理路径未统一，且 Docker Hub 网络不稳定。
- 处理：
  - Docker Desktop 配置 HTTP/HTTPS 代理。
  - 网络不稳定时逐个拉取基础镜像。
  - 完成后依靠本地镜像缓存。
- 结果：MySQL、Mosquitto、Node、Nginx、Maven、JRE 镜像齐全并完成构建。
- 后续：正式发布成品镜像，并评估国内容器镜像仓库。

### Docker Compose 一键部署

- 新增前后端多阶段 Dockerfile。
- 新增 MySQL、Mosquitto、后端和前端 Compose 编排。
- Nginx 提供前端并反向代理 `/api`。
- 后端数据库和 MQTT 地址支持环境变量覆盖，继续兼容本地开发。

## 2026-07-08

### MQTT 正式化

- 后端使用 Eclipse Paho 订阅 `aiot/device/+/report`。
- MQTT 消息复用 HTTP 设备上报业务逻辑。
- 新增 MQTT 状态接口和前端状态页面。
- 增加消息参数校验、接收统计、失败统计和最近错误。
- 模拟脚本改用 UTF-8 临时文件发布 JSON，避免 PowerShell 参数传递丢失双引号。
- Mosquitto 本机真实发布/订阅联调通过。

### 第二阶段设备自动上报

- 完成 HTTP 上报接口和 `device_reports` 表。
- 异常上报自动生成来源为 `DEVICE` 的日志。
- 日志列表支持来源筛选和中文来源展示。
- 设备详情展示最近上报与温度、湿度、电压、信号趋势。
- 完成告警规则新增、编辑、删除和启停。
- 告警判断优先使用配置规则，没有规则时使用默认阈值。

### 前端交互调整

- 日志筛选修改后自动刷新。
- 调整时间筛选栏宽度，避免超出页面。
- 删除与自动筛选重复的刷新操作。
- 左侧菜单固定，右侧内容独立滚动。

## 2026-07-07

### 第一阶段功能验收

- 设备、日志、标签 CRUD 验证通过。
- 日志筛选、状态修改和首页统计验证通过。
- Vue 3 前端与 Spring Boot/MySQL 完成真实数据联调。

### 本地一键启动与停止

- 新增 `start-all.cmd/.ps1` 和 `stop-all.cmd/.ps1`。
- 启动脚本管理 MySQL、后端、前端和 Mosquitto。
- 修复浏览器打开时机、桌面快捷脚本工作目录和停止后页面仍保留等使用问题。

### 前端工程

- 创建 Vue 3 + TypeScript + Vite 项目。
- 集成 Element Plus、Pinia、Vue Router、Axios。
- 建立 Dashboard、设备、日志和标签页面。
- `/api` 由 Vite 代理到 Spring Boot `8080`。

### 后端与数据库联调

- 使用 Java 17、Spring Boot 3.3.5、MyBatis Plus。
- 完成设备、日志、标签、统计和枚举接口。
- MySQL 使用项目本地数据目录和初始化 SQL。
- 处理 JDBC 连接参数、Maven 本地仓库和 Windows JAR 占用问题。

## 2026-07-06

### 项目设计与后端基础

- 明确第一阶段需求、页面、枚举和验收标准。
- 完成数据库表、索引和关系设计。
- 完成 REST API 与前端页面结构设计。
- 创建 Spring Boot 基础工程、统一响应、异常处理和 MyBatis Plus 配置。

## 维护规则

- 新记录放在最上方对应日期下。
- 不使用跨日期连续序号。
- 每个故障使用“问题、根因、处理、验证”结构。
- 当前状态写入 `project-status.md`，未来任务写入 `future-development-backlog.md`。
- 只有需要追溯完整原始过程时才解压 `history/docs-before-consolidation-2026-07-09.zip`。

## 通用排错方法

按数据链路从外到内检查，不要同时修改多个位置：

1. 页面是否能打开：检查前端/Nginx。
2. `/api` 是否返回：检查代理和 Spring Boot。
3. 后端是否报连接错误：检查 MySQL、MQTT 和环境变量。
4. 数据是否入库：直接查询 MySQL。
5. 数据库正确但页面错误：检查 API 响应、前端字段和编码。
6. 仅 Docker 失败：比较容器地址与 `127.0.0.1`，容器之间应使用服务名。

常用命令：

```powershell
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
docker compose logs -f mosquitto
```

问题类型判断：

| 现象 | 优先检查 |
| --- | --- |
| 页面无法访问 | 前端进程、Nginx、端口 |
| 页面出现 502 | 后端未就绪、Nginx 代理 |
| API 500 | 后端日志、数据库 |
| MQTT 未连接 | Broker、地址、端口、订阅配置 |
| 镜像拉取失败 | Docker Hub 网络、代理 |
| 页面中文乱码 | 数据库存储、HTTP 原始字节、响应编码 |

学习总结：先确认问题属于哪一层，再查看该层日志；能够稳定复现后再修改，修改后用同一路径重新验证。
## 2026-07-16 远程日志等级失败与页面刷新延迟

- 现象：MQTT 状态页显示消息已收到但部分处理失败，最近错误为“日志等级不合法”；日志管理页标称自动刷新但合并中的运行日志看起来没有变化。
- 根因一：小智使用 `WARN`，IoT 后端只接受 `WARNING`。消息已经通过 HiveMQ TLS 到达后端，失败发生在入库校验，不属于设备网络或 Broker 故障。
- 根因二：状态页和日志页原轮询周期为 5 秒；日志页自动刷新会触发表格加载状态，查询也未显式禁止缓存。运行日志按 `createdAt` 排序和展示时，30 秒窗口内更新同一行不会改变创建时间，容易看起来没有刷新。
- 修复：设备日志入口将 `WARN` 规范化为 `WARNING`；两个页面统一 1 秒轮询并增加请求互斥、无缓存请求和 Nginx `no-store`；日志自动请求取消遮罩，列表按 `updatedAt`、`id` 倒序并显示更新时间。
- 验证：JDK 17 Maven 测试 8 项全部通过，前端生产构建通过。未读取真实 HiveMQ 配置；后续以独立标签 `v1.1.3-remote-mqtt` 发布，不覆盖旧版本，也不自动部署群晖。
