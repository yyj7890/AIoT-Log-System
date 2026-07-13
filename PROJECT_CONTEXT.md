# AIoT 项目上下文

更新时间：2026-07-13

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

待处理：实机曾在同一汇总窗口收到两轮相同启动序列，形成“设备运行上报（8 条）”；IoT 后端按收到消息如实合并，需小智固件保证每次启动仅发布一轮。

- 设备运行事件统一中文显示：后端写入时转换已知事件，前端也兼容转换历史英文记录；包括 Wi-Fi、日志 MQTT 连接/失败重试/恢复、本地 AI WebSocket 握手、官方 AI 协议状态；日志标题单行省略并提供完整内容提示
- 连续运行事件汇总：同一设备在 30 秒内连续发布到 `/log` 的事件仅保留一条“设备运行上报（n 条）”日志，内容逐行保存每一步的最简状态；详情页以“固件开始初始化 → Wi-Fi 已连接 → …”展示，避免标题和消息重复。`startup`/`firmware_started` 是新的启动批次边界，即使设备复位发生在 30 秒内也必须新建一条；其余事件超过窗口才新建日志。已由 `XIAOZHI-001` 实机验证 4 条启动事件成功合并，并验证电脑与小智同连手机热点时可自动发现 Broker 并正常上报；断网后可自动重连并恢复日志上传。

- 设备、日志、标签管理
- 日志状态、级别、类型、来源筛选
- 日志管理页自动刷新当前筛选和分页结果：页面可见时每 5 秒拉取一次，切走页面或浏览器标签页隐藏时停止
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

Docker 联调已通过：

- frontend、backend、mysql、mosquitto 四容器正常运行
- MySQL 健康检查通过
- 网页与后端接口返回正常
- MQTT 后端连接正常
- Docker 初始化数据中文乱码已修复
- Docker 启动脚本会等待后端和前端就绪后再打开浏览器

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

Docker 公开部署策略：`docker-update.cmd`/`docker-start.cmd` 会调用 `tools/initialize-docker-config.ps1`，在被忽略的 `.env` 和 `docker/local/` 生成或复用数据库密码、MQTT 凭证、发现 Token、Mosquitto 密码/ACL 和当前局域网 IPv4。Docker Mosquitto 关闭匿名访问，后端从该私有目录读取 MQTT 凭证，并发布 UDP `19830` 供局域网设备发现；没有可用局域网 IPv4 时仍可启动，但会暂时关闭发现。该更新尚未在本机重新构建验收；不能与本地开发版同时启动，也不得暴露至公网。

GHCR 成品镜像策略：`.github/workflows/publish-ghcr.yml` 在 `main` 推送、版本标签或手动触发时构建并推送后端、前端镜像到 `ghcr.io/yyj7890/aiot-log-backend` 与 `ghcr.io/yyj7890/aiot-log-frontend`。`docker-compose.ghcr.yml` 和 `docker-ghcr-*.cmd` 仅拉取成品镜像，使用者无需本机编译源码；首次成功发布后仍需仓库维护者在 GitHub Packages 将两个包设为 Public，其他人才能匿名拉取。尚未实际推送工作流，因此镜像尚未生成。

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

1. 按 `screenshots/README.md` 准备并脱敏 GitHub 展示截图，按 `SECURITY-CHECKLIST.md` 完成发布前复核。
2. 发布前后端成品镜像，并考虑同步到国内容器镜像仓库。
3. 后续再考虑管理网页登录、HTTP 设备认证、每设备 MQTT 凭证、TLS、OpenAPI、测试、在线演示和 AI 分析。

延期观察：本地 AI 取消/不可用后回退官方 AI 偏慢，以及本地 AI 能力弱，均已记录但暂不处理；后续如决定优化，需在小智固件/本地 AI 服务项目进行。

## 8. 已知注意事项

- Docker Hub 在当前网络下可能需要 VPN/代理；基础镜像已在本机缓存。
- Docker 构建 Maven 依赖使用 `backend/maven-settings-docker.xml` 国内镜像和 BuildKit 缓存。
- 当前 Mosquitto 已关闭匿名访问并使用全局 MQTT 凭证；仍仅限可信局域网，禁止直接暴露公网，后续公网部署需启用 TLS 和每设备独立 ACL。
- 本地 ESP32 联调使用 `config/mosquitto-lan.conf`；Mosquitto 监听本机所有接口，Windows 已创建仅限专用网络和本地子网的 TCP `1883` 入站规则，避免向公网开放。已完成本机 MQTT 发布订阅、UDP 发现和首次真实 ESP32 上报验证。
- SQL 初始化脚本必须保持 UTF-8，并显式执行 `SET NAMES utf8mb4`。
- Docker 数据保存在命名卷中，`docker compose down` 不删除数据；`down -v` 会删除数据。
