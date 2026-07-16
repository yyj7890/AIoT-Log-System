# 开发记录

本文件记录关键里程碑和可复用的故障处理方法，按日期倒序排列。

完整早期过程保存在 `../history/docs-before-consolidation-2026-07-09.zip`，包括原始命令、长篇报错和逐步搭建过程。

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
