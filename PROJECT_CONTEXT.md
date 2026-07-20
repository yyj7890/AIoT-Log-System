# AIoT 项目上下文

更新时间：2026-07-20

这是新对话继续项目时的首要阅读文件。

## 1. 项目目标

通用 AIoT 设备运行日志管理系统，用于管理设备、运行日志、设备上报数据、告警规则，并通过 HTTP/MQTT 接收真实设备数据。小智 ESP32-S3 是当前完成实机验证的设备接入示例，不是系统唯一或主体设备类型。

## 2. 技术栈

- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios
- 后端：Java 17、Spring Boot 3.3.5、MyBatis Plus
- 数据库：MySQL 8.4
- MQTT：Mosquitto 2、Eclipse Paho
- 部署：Docker Compose、Nginx

## 3. 已完成功能

- `Local AI server discovered` 统一显示为“已发现本地 AI 服务”，仅表示发现成功，不能被翻译为已连接；只有 `local_ai_connected` / `Local AI server connected` 才显示为“已连接本地 AI 服务”。2026-07-12 曾实机验证设备在本机服务可用时建立 WebSocket 实际对话；本地服务不可用时固件应安全回退官方小智服务。

2026-07-16 已定位并修复远程固件启动汇总中的 MQTT 假异常：等待可信系统时间、主动停止客户端不再算连接失败，恢复后不再重复补发“已连接”，真实意外断开会在后台主动重连。修复版标识为 `v2.2.6-aiot-remote-mqtt.2`，尚待重新烧录验收。

- 设备运行事件统一中文显示：后端写入时转换已知事件，前端也兼容转换历史英文记录；包括 Wi-Fi、日志 MQTT 连接/失败重试/恢复、本地 AI WebSocket 握手、官方 AI 协议状态；日志标题单行省略并提供完整内容提示
- 连续运行事件汇总：同一设备在 30 秒内连续发布到 `/log` 的事件仅保留一条“设备运行上报（n 条）”日志，内容逐行保存每一步的最简状态；详情页以“固件开始初始化 → Wi-Fi 已连接 → …”展示，避免标题和消息重复。`startup`/`firmware_started` 是新的启动批次边界，即使设备复位发生在 30 秒内也必须新建一条；其余事件超过窗口才新建日志。已由 `XIAOZHI-001` 实机验证 4 条启动事件成功合并，并验证电脑与小智同连手机热点时可自动发现 Broker 并正常上报；断网后可自动重连并恢复日志上传。

- 设备、日志、标签管理
- 日志状态、级别、类型、来源筛选
- 日志管理页自动刷新当前筛选和分页结果：页面可见时每 1 秒拉取一次，切走页面或浏览器标签页隐藏时停止；自动请求不显示表格遮罩，使用请求互斥和禁止缓存参数，并按最近更新时间排序，使 30 秒汇总窗口内更新的运行日志也能及时回到顶部。详情弹窗打开时会同步当前页同 ID 的最新对象；记录不在当前页时使用详情接口静默兜底，避免重复并发，记录被删除时安全关闭弹窗
- 2026-07-20 修复日志页首次加载请求失败后轮询永远不启动的问题：轮询和页面恢复监听现在先于网络请求建立，页面重新可见、窗口聚焦、网络恢复或浏览器恢复时会自愈重启轮询并立即刷新；初始设备/标签选项失败不再阻断日志刷新
- 顶部状态区显示 Spring Boot 后端实际运行时长，格式为“天 + 时:分:秒”；前端每秒本地递增、每分钟与轻量 `/api/system/runtime` 接口校准，页面恢复或网络恢复时立即重新同步
- 日志管理页在点击“批量删除”后才显示勾选列，支持当前页全选、跨页保留选择和批量删除；完成或取消时会清空表格内部选择状态，避免旧日志编号再次提交
- 首页统计和设备详情
- HTTP 设备上报：`POST /api/device-reports`
- MQTT 订阅：`aiot/device/+/report`
- MQTT 运行日志订阅：`aiot/device/+/log`，写入 `logs` 表并标记为 `DEVICE` 来源
- MQTT 认证已启用为全局账号模式：MQTT 状态页可注册/修改一套全局设备账号密码，并写入本机 Mosquitto 密码文件与通用 Topic ACL；小智已在配网页填写同一套凭证并完成认证上报验证。设备留空或错误时仅停止日志上传。新建认证的标准流程为：先在 IoT 创建凭证、关闭匿名访问并重启服务使认证生效，再在设备配网页填写凭证；已有匿名设备批量迁移时，也可先保存设备凭证以避免切换瞬间断日志。
- 正常/异常模拟上报脚本
- 异常上报自动生成来源为 `DEVICE` 的日志
- 最近上报数据和温度、湿度、电压、信号趋势
- 告警规则管理
- MQTT 状态接口和页面
- 左侧菜单固定、内容区独立滚动
- 本地一键启动/停止
- 本地启动脚本端口就绪轮询间隔已优化为 200 毫秒；后端 Java 初始化通常仍需约 2 秒，这是正常启动时间。
- Docker Compose 四服务部署；2026-07-13 已更新为首次运行自动生成本机私有 `.env` 与 `docker/local/`，不再使用公开默认数据库密码或匿名 MQTT

## 4. 当前状态

Docker 联调与群晖 NAS 成品镜像部署已通过：

- frontend、backend、mysql、mosquitto 四容器正常运行
- MySQL 健康检查通过
- 网页与后端接口返回正常
- MQTT 后端连接正常
- Docker 初始化数据中文乱码已修复
- Docker 启动脚本会等待后端和前端就绪后再打开浏览器
- 2026-07-14 已在群晖 DSM Container Manager 完成 GHCR 前后端成品镜像、离线导入 MySQL/Mosquitto 基础镜像、私有配置初始化、端口冲突调整、Mosquitto 权限修复和 UDP `19830` 自动发现实机验收；详见 `docs/synology-nas-deployment.md`

当前以小智 AI 真实硬件作为设备接入示例，已完成本地与官方 AI 双通道实机验证：本地服务可用时，设备经局域网发现、OTA 配置下发和 WebSocket 会话实际使用本地 AI；本地服务不可用时回退官方 AI。独立 MQTT 日志通道已接入本地日志系统，`XIAOZHI-001` 可持续发布状态和运行事件。其他设备仍可通过通用 MQTT、HTTP 或协议适配器接入。

真实设备接入采用以下分工：

- 官方 WebSocket/MQTT 通道继续负责 AI 对话。
- 修改匹配板型的固件，新增独立本地 MQTT 遥测通道：周期状态发布到 `report`，关键运行事件发布到 `log`。
- IOT 已提供 UDP `19830` 的 MQTT Broker 发现响应；固件在同一局域网内优先通过自动发现获取 Broker，发现失败时使用手动配置的 Broker 地址作为联调兜底。小智固件发现客户端已烧录并完成首次真实上报验证。
- 小智已完成 `/log` 关键事件真实联调：`startup`、`wifi_connected`、`mqtt_connected`、`official_protocol_connected` 均可写入 `DEVICE` 日志。启动早期事件的固件时间尚未同步，当前会显示为 `1970-01-01`；固件应在时间同步前省略 `reportedAt`，由后端使用接收时间。
- USB 串口仅用于开发阶段查看详细调试日志，不作为生产日志主通道。
- 不同设备优先通过通用 MQTT、HTTP 或串口网关接入；私有协议由适配器转换为统一事件格式。

项目文档已经整理：新对话读取本文件；当前进度、专题设计和历史归档由 `docs/README.md` 分类导航。

GitHub 展示与交付材料已整理：根目录 `README.md` 已覆盖项目说明、启动方式、真实小智接入、MQTT 安全、验证范围和限制；`docs/architecture.mmd` 提供可维护架构图源；`screenshots/README.md` 列出待准备截图与脱敏要求；`SECURITY-CHECKLIST.md` 用于上传前检查。截图尚需人工按清单准备，真实凭证、MAC/IP、运行日志和数据库数据不得提交。

公开发布配置策略：仓库提交 `backend/src/main/resources/application.example.yml`、`config/mosquitto-lan.example.conf`、`config/mosquitto-acl.example.conf` 与 `config/mqtt-credentials.env.example`；实际本地配置与凭证保留在原位置并忽略。注意：`.gitignore` 不会移除已有 Git 历史中的文件，首次公开推送前必须确认历史不包含本地配置或改用干净公开历史。

Docker 公开部署策略：`docker-update.cmd`/`docker-start.cmd` 会调用 `tools/initialize-docker-config.ps1`，在被忽略的 `.env` 和 `docker/local/` 生成或复用数据库密码、MQTT 凭证、Mosquitto 密码/ACL 和当前局域网 IPv4。Docker Mosquitto 关闭匿名访问，后端从该私有目录读取 MQTT 凭证，并发布 UDP `19830` 供局域网设备发现；发现 Token 默认留空，确保未预置 Token 的设备可自动发现，固件已配置同一 Token 时才在私有 `.env` 手动启用。没有可用局域网 IPv4 时仍可启动，但会暂时关闭发现。新用户 Docker 首次启动仅执行 `sql/schema.sql`，不再自动导入 `sql/init-data.sql` 的演示设备/日志/标签。新增 `tools/initialize-synology-docker-config.sh` 供群晖 DSM SSH 首次初始化；Container Manager 不会执行 Windows 脚本。群晖脚本会将 Mosquitto 密码哈希和 ACL 设为容器 UID/GID `1883` 所有、权限 `0600`，否则 Broker 无法读取安全文件；Docker 后端从页面更新全局凭证时也会保持这一权限。已拉取 GHCR 前后端成品镜像时，可用 `tools/create-synology-image-deploy.ps1` 生成只含 Compose、表结构 SQL 与初始化脚本的群晖部署包，无须上传 `backend/`/`frontend/` 源码。该更新尚待在本机或群晖重新构建验收；不能与本地开发版同时启动，也不得暴露至公网。

工程质量版本 `v1.1.6-remote-mqtt`：后端已接入 Spring Boot Actuator，仅开放不含组件细节的 `health` 端点，并将管理端口默认固定为只监听 `127.0.0.1:8081`；应用默认值同时保护仍使用旧私有 `application.yml` 的本地环境，Docker 不映射该端口。源码构建的局域网和远程 Compose 使用容器内部健康检查，前端等待后端达到 `service_healthy` 后再启动；远程 GHCR Compose 已固定到 `v1.1.6-remote-mqtt`。2026-07-20 已完成远程前后端镜像构建、JDK 17 Maven 14 项后端测试和隔离容器验收：健康端点返回 `UP`，同一 Docker 网络中的其他容器不能访问管理端口。提交 `c853d6c`、标签、GitHub Release 和两个 GHCR 镜像均已发布；群晖当前仍运行 `v1.1.5-remote-mqtt`，尚未升级。

GHCR 成品镜像策略：`.github/workflows/publish-ghcr.yml` 在 `main` 或 `Remote-Hivemq` 推送、版本标签或手动触发时构建并推送后端、前端镜像到 `ghcr.io/yyj7890/aiot-log-backend` 与 `ghcr.io/yyj7890/aiot-log-frontend`。两个包以标签区分版本：`v1.0.0-lan` 是固定局域网版，`v1.1.0-remote-mqtt` 是首个远程版，`v1.1.1-remote-mqtt` 是远程状态文案和 QoS 1 相邻重投修复版，`v1.1.2-remote-mqtt` 增加 MQTT 状态页自动刷新，`v1.1.3-remote-mqtt` 增加 `WARN` 等级兼容并统一状态/日志列表为 1 秒可靠刷新，`v1.1.4-remote-mqtt` 增加详情弹窗实时同步和本地 AI 未发现/回退官方 AI 中文映射，`v1.1.5-remote-mqtt` 增加日志轮询自愈和系统运行时长显示，`v1.1.6-remote-mqtt` 增加容器内部后端健康检查和前端就绪依赖。2026-07-20 已从提交 `c853d6c` 发布 `v1.1.6-remote-mqtt`，GitHub Release 和两个公开 GHCR 镜像清单均已验证可用。`latest` 只允许 `main` 分支推送更新并保持局域网语义，版本标签事件不得更新 `latest`。`docker-compose.ghcr.yml` 和 `docker-ghcr-*.cmd` 仅拉取成品镜像，使用者无需本机编译源码。包仍应保持 Public，便于其他用户匿名拉取。

HiveMQ 远程版本：Git 分支 `Remote-Hivemq` 已实现可选 `MQTT_MODE=remote`。该模式使用 `ssl://<private-host>:8883`、共享 MQTT 凭证和 HiveMQ `aiot/device/#` ACL；后端继续订阅原有 `report`/`log` Topic，Docker/GHCR 远程编排只运行 MySQL、后端和前端，不运行 Mosquitto 或 UDP `19830`。远程 Docker 配置只存于被忽略的 `docker/local/hivemq-remote.env`，与局域网 `.env` 分离，模板是 `config/hivemq-remote.env.example`；Windows 使用 `docker-remote-*.cmd`，群晖成品部署包使用 `tools/create-synology-image-deploy.ps1 -Remote`。源码中的远程 GHCR Compose 已固定使用 `v1.1.6-remote-mqtt`，群晖当前仍运行 `v1.1.5-remote-mqtt`；旧版本标签继续保留用于回退，远程群晖 Compose 不拉默认分支的 `latest`。群晖升级继续复用原 MySQL 数据卷和私有环境文件；2026-07-20 已确认系统运行时长逐秒增加、MQTT 保持已连接、小智重启日志无需切换 MQTT 状态页即可约 1 秒自动显示，浏览器标签隐藏后返回也会立即补刷。小智独立日志客户端增加远程 TLS 模式、ESP-IDF CA bundle、主机名验证/SNI 和跳过 UDP 发现的逻辑；官方 AI/OTA/WebSocket 通道未修改。2026-07-15 已分别在 Windows Docker 和群晖 Container Manager 完成 MQTTX → HiveMQ TLS → Spring Boot → MySQL 远程日志端到端入库验收；2026-07-16 小智真机也已成功经 HiveMQ 上传日志，确认固件 TLS 与后端订阅链路可用。首次固件出现的 MQTT 失败/恢复假异常已在 `.2` 修复版中处理；后端收到明确恢复事件时会把汇总状态改为 `RESOLVED`，真实后续故障会重新置为 `PENDING`。IoT 后端和前端已补充远程 TLS 连接、失败重试、连接恢复三种中文文案，其中连接成功显示为“远程日志 MQTT（TLS 8883）已连接”；后端在同一启动批次和 30 秒汇总窗口内仅抑制相邻且标准化后完全相同的运行事件，继续保持 QoS 1，并保留原事件类型与故障状态流转。2026-07-16 进一步修复小智 `WARN` 与 IoT `WARNING` 的等级枚举不一致：后端设备日志入口兼容大小写并把 `WARN` 规范化为 `WARNING`，现有固件无需重刷。MQTT 状态页与日志管理页统一为页面可见时每 1 秒自动刷新，隐藏标签页时暂停，返回时立即更新，同时保留手动刷新。2026-07-17 日志详情弹窗已跟随列表轮询实时同步，不在当前页时使用无缓存详情接口兜底；本地 AI 未发现和正常回退官方 AI 的两类事件也已增加前后端中文映射，且不参与 MQTT 故障状态逻辑。详见 `docs/hivemq-remote-mqtt-version.md`。

## 5. 启动方式

本地开发版：

```text
start-all.cmd
stop-all.cmd
网页：http://127.0.0.1:5173/
```

Docker 版：

```text
docker-start.cmd   日常快速启动
docker-update.cmd  首次部署或代码更新后重新构建
docker-stop.cmd    停止并保留数据
网页：http://127.0.0.1/
```

本地版和 Docker 版会争用 `3306`、`8080`、`1883`、UDP `19830`，不要同时启动。

## 6. 关键路径

- 后端：`backend/`
- 前端：`frontend/`
- SQL：`sql/`
- 模拟脚本：`tools/`
- Docker 编排：`docker-compose.yml`
- 常用文档：`docs/README.md`
- 文档导航：`docs/README.md`
- 项目设计：`docs/project-design.md`
- 当前进度：`docs/project-status.md`
- 故障与修复：`docs/development-log.md`

## 7. 下一步

优先顺序：

0. `v1.1.6-remote-mqtt` 已发布；下一步使用原 MySQL 数据卷和私有环境文件升级群晖，并确认后端容器变为 `healthy`、前端在后端健康后启动。
1. 继续观察群晖 `v1.1.5-remote-mqtt` 的长时间运行，并在后续后端正常重启时核对顶部系统运行时长归零；首次 API 请求失败场景出现时确认轮询能够自行恢复。
2. 局域网 UDP `19830` 回归和 GitHub 脱敏截图已按用户当前安排暂缓，后续恢复时仍须遵守端口隔离与安全检查清单。
3. 后续再考虑管理网页登录、HTTP 设备认证、每设备 MQTT 凭证、OpenAPI、数据库迁移、在线演示和 AI 分析。

延期观察：本地 AI 取消/不可用后回退官方 AI 偏慢，以及本地 AI 能力弱，均已记录但暂不处理；后续如决定优化，需在小智固件/本地 AI 服务项目进行。

## 8. 已知注意事项

- Docker Hub 在当前网络下可能需要 VPN/代理；基础镜像已在本机缓存。
- Docker 构建 Maven 依赖使用 `backend/maven-settings-docker.xml` 国内镜像和 BuildKit 缓存。
- 当前 Mosquitto 已关闭匿名访问并使用全局 MQTT 凭证；仍仅限可信局域网，禁止直接暴露公网，后续公网部署需启用 TLS 和每设备独立 ACL。
- 本地 ESP32 联调使用 `config/mosquitto-lan.conf`；Mosquitto 监听本机所有接口，Windows 已创建仅限专用网络和本地子网的 TCP `1883` 入站规则，避免向公网开放。已完成本机 MQTT 发布订阅、UDP 发现和首次真实 ESP32 上报验证。
- SQL 初始化脚本必须保持 UTF-8，并显式执行 `SET NAMES utf8mb4`。
- Docker 数据保存在命名卷中，`docker compose down` 不删除数据；`down -v` 会删除数据。
