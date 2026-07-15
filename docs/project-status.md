# 当前项目状态

更新时间：2026-07-15

## 当前阶段

2026-07-15：`Remote-Hivemq` 分支已实现 HiveMQ Cloud 远程 MQTT 模式，且未替换默认局域网模式。后端可通过 `MQTT_MODE=remote` 使用 Paho `ssl://` TLS 订阅原有 `report`/`log` Topic，远程时强制关闭 UDP `19830` 响应器、隐藏远程 Broker 地址并禁止页面修改本地 Mosquitto 凭证。新增源码/GHCR 远程 Compose、Windows 启停脚本、群晖私有 `docker/local/hivemq-remote.env` 初始化和远程镜像部署包生成选项；该文件与局域网 `.env` 分离。远程编排不包含 Mosquitto、不映射 `1883` 或 UDP `19830`。远程版以固定 GHCR 标签 `v1.1.0-remote-mqtt` 发布，局域网版固定为 `v1.0.0-lan`；`latest` 保持局域网语义并只由 `main` 分支推送更新。小智独立日志客户端已增加持久化远程 TLS 配置、ESP-IDF CA bundle、域名验证/SNI 和跳过 UDP 发现，未修改官方 AI、OTA 或 WebSocket 通道。真实 HiveMQ 信息未读取或写入项目。

远程版本已完成的外部前置验证：用户使用 MQTTX 成功验证 TLS `8883` 连接、`aiot/device/#`（QoS 1）订阅，以及向 `aiot/device/TEST-001/log`（QoS 1）发布并接收 JSON 测试日志；未记录真实域名或凭证。

远程版本已完成 Windows 端到端验收：远程 Compose 的 MySQL、后端和前端均已启动，后端状态页显示远程 TLS 已连接。MQTTX 发布的有效 `/log` 消息已由 Spring Boot 成功处理并写入新建 MySQL 数据库；测试同时确认 Topic、Payload `deviceCode` 与已创建设备编号必须一致，`logType` 必须使用 `RUNNING`、`ERROR`、`MAINTENANCE` 或 `INSPECTION`。

远程版本已完成群晖实机验收：使用固定标签 `v1.1.0-remote-mqtt` 在 DSM Container Manager 创建独立远程项目，MySQL、后端和前端启动成功，后端通过 HiveMQ TLS 连接后收到了 MQTTX 测试消息并处理成功。NAS 的 Docker CLI 不带 Compose V2 插件，因此通过 Container Manager 创建项目；宿主机 `8080` 被既有服务占用后，将远程后端和前端宿主机端口分别调整为 `18080` 和 `18000`，容器内部 `backend:8080` 通信不变。未记录真实 NAS 地址或 HiveMQ 凭证。

远程版本尚待验收：不同网络下 ESP32 真机 TLS 日志上报，以及切回 `lan` 后的 UDP 本地模式回归。

构建与静态验证：后端已在 JDK 17 下执行 `mvn -s maven-settings-docker.xml -DskipTests package` 并成功；前端生产构建、远程 Compose 解析、PowerShell 脚本语法、Markdown 本地链接与 `git diff --check` 均通过。ESP-IDF 完整构建仍待本机补齐其 Python 虚拟环境后执行。

当前结论（2026-07-13）：真实小智已完成普通 Wi-Fi、手机热点、断网恢复、本地 MQTT 日志汇总、官方/本地 AI 动态切换和本地 AI WebSocket 实际对话验证。IoT 全局 MQTT 凭证已创建并启用：匿名访问已关闭，服务重启后小智使用配网页保存的同一套凭证完成日志上报验证。

补充验收（2026-07-13）：本地 AI 服务取消/不可用时，设备最终能够自动回退官方 AI；当前已知体验问题是回退等待偏长。本地 AI 回答能力也弱于官方 AI。两项均需在小智固件/本地 AI 服务项目处理（本地发现、WebSocket 连接超时和重试策略；本地 LLM Provider、模型与提示词配置），IoT 端仅负责记录对应状态日志。

补充（2026-07-12）：`Local AI server discovered` 映射为“已发现本地 AI 服务”，只描述发现结果，不能作为实际连接状态；`local_ai_connected` / `Local AI server connected` 才映射为“已连接本地 AI 服务”，`local_ai_connection_failed` / `Local AI server connection failed` 映射为“本地 AI 服务连接失败”。实机曾在本机服务可用时建立 WebSocket 实际对话；本地服务不可用时固件应安全回退官方服务。实机曾在同一 30 秒窗口收到两轮相同启动事件而形成“设备运行上报（8 条）”；IoT 后端未自行重复生成，待小智固件确认本地日志客户端不重复初始化或重复发布启动序列。

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

- 设备、日志、标签、告警规则管理
- 首页统计、设备详情和日志筛选
- 日志管理页每 5 秒自动刷新当前筛选和分页结果；页面隐藏或离开时停止，返回标签页时立即刷新一次
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
- 真实小智已发布 `startup`、`wifi_connected`、`mqtt_connected`、`official_protocol_connected` 日志并成功入库；前三类启动早期事件时间暂显示 `1970-01-01`，等待固件在未同步时间时省略 `reportedAt`
- 已执行 `sql/runtime-log-localization.sql` 以中文化历史运行日志；前端可即时兼容展示英文历史记录。当前运行中的高权限 Java 后端无法由普通会话重启，因此需管理员重启后端后，新入库日志才会直接保存为中文。
- MQTT 全局凭证管理和关闭匿名访问已完成实机验收；HTTP 设备身份认证和管理网页登录尚未实现
- 已验证 GHCR 成品镜像在群晖 NAS 拉取和运行；尚未提供在线演示地址
- 登录权限、自动化测试和 AI 分析尚未开发

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

尚未验证：

- 真实硬件长时间持续上报
- 网络断开后的设备重连和数据补偿
- 多设备并发和大数据量性能
- 身份认证、权限和恶意请求防护
- 自动化回归测试和生产服务器部署

## 当前风险与技术债

- 当前全局 MQTT 凭证和无 TLS 配置不能用于公网；生产环境需使用 TLS、每设备凭证和最小权限 ACL。
- HTTP 上报可被伪造，缺少设备密钥或签名。
- 数据库变更仍依赖初始化 SQL，尚未使用 Flyway/Liquibase。
- 前后端自动化测试覆盖不足。
- 前端主包体积较大，尚未进一步拆包。
- 国内镜像仓库尚未完成。
- 群晖部署已完成首次验收，但仍需进行长时间稳定性、端口冲突覆盖和升级回归验证；详见 `synology-nas-deployment.md`。
- GHCR 包应保持 Public；尚未准备发布标签、镜像签名或漏洞扫描。

## 下一步

1. 按截图清单准备已脱敏的首页、设备详情、中文运行日志、MQTT 认证状态和批量删除模式截图；上传前执行安全检查清单。
2. 确认固件不会重复发布同一轮启动事件，并持续观察启动汇总日志。
3. 完成群晖环境下的长时间持续上报、断网恢复和端口冲突升级回归验证。

延期观察：本地 AI 回退官方 AI 偏慢、以及本地 AI 回答能力弱，均已记录但暂不处理；以后如决定优化，需在小智固件/本地 AI 服务项目执行。

详细任务见 `future-development-backlog.md`。
