# 群晖 NAS 实际部署记录

更新时间：2026-07-21

本文记录一次已完成的群晖 DSM Container Manager 实机部署，用于后续同类 NAS 部署排障。文中不包含真实 IP、域名、账号、密码、Token、设备 MAC 或运行数据。

## 1. 验收结果

- GHCR 前端、后端成品镜像已在群晖拉取并运行，无需在 NAS 上传或构建 `frontend/`、`backend/` 源码。
- `frontend`、`backend`、`mysql:8.4`、`eclipse-mosquitto:2` 四个容器均已启动；MySQL 健康检查通过，管理网页和后端可访问。
- 新部署的数据库仅创建表结构，不自动导入演示设备、日志和标签。
- Mosquitto 已启用全局 MQTT 凭证和 ACL；Broker、后端订阅及设备日志链路可用。
- ESP32 已通过 UDP `19830` 自动发现 NAS 上的 MQTT Broker 并成功连接；手动 Broker 地址仅作为发现失败时的兜底。

## 2. 成品镜像部署方式

使用 `tools/create-synology-image-deploy.ps1` 生成 `dist/aiot-synology-image-deploy.zip`。部署包仅包含：

```text
docker-compose.yml
sql/schema.sql
tools/initialize-synology-docker-config.sh
README.txt
```

其中 Compose 引用 GHCR 的前端、后端镜像。NAS 不需要 Java、Node.js、Maven，也不需要项目源码。

首次部署在 NAS 项目目录执行：

```sh
sh tools/initialize-synology-docker-config.sh
```

脚本生成被忽略的 `.env` 与 `docker/local/`，包括数据库密码、MQTT 凭证、Mosquitto 密码哈希、ACL 和局域网发现地址。文件不得上传或提交。

## 3. 实际问题与处理

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| Docker Hub 拉取 MySQL/Mosquitto 超时或 EOF | NAS/本地网络到 Docker Hub CDN 不稳定 | 在可访问 Docker Hub 的电脑拉取基础镜像后使用 `docker save` 导出，再从 Container Manager 导入。 |
| Container Manager 报绑定挂载失败 | 缺少 SQL 或私有 Mosquitto 配置文件；Windows 路径分隔符 ZIP 曾被 DSM 当作普通文件名 | 使用群晖部署包和初始化脚本；部署包 ZIP 已改为使用 `/` 路径。 |
| 后端或前端启动时报 external connectivity | NAS 中已有容器或服务占用默认端口 | 在私有 `.env` 更换冲突端口，例如 `BACKEND_PORT`、`WEB_PORT`；容器内部仍通过 `backend:8080` 通信。 |
| Mosquitto 循环退出且提示无法打开 password file | DSM 绑定文件由 root 所有，容器内 `mosquitto` 用户无法读取 | 将 `mosquitto-passwords` 和 `mosquitto-acl.conf` 设为 UID/GID `1883` 所有、权限 `0600`；初始化脚本和页面保存逻辑已修正。 |
| 设备只能手动填写 Broker 地址，不能自动发现 | 初始化曾生成发现 Token，但设备未预置同一 Token，服务端拒绝请求 | 默认发现 Token 改为空；设备同网段且无客户端隔离时可自动发现。若固件已预置 Token，可在私有 `.env` 手动设置。 |

## 4. 端口与网络边界

- MQTT：TCP `1883`。
- 自动发现：UDP `19830`。
- 后端和网页端口应按 NAS 已有服务情况在私有 `.env` 调整；不应假定 `8080` 或 `80` 可用。
- 设备与 NAS 必须在可互访的同一局域网；路由器、热点或 AP 的客户端隔离会阻断 UDP 自动发现。
- 不得将 `3306`、`1883`、`19830/UDP` 暴露至公网；当前未启用 TLS 和每设备独立 ACL。

## 5. 凭证与设备配置

MQTT 状态页保存的全局凭证会写入 NAS 的 Broker 配置，但不会自动下发到设备配网页。设备仍需保存同一套 MQTT 用户名和密码；自动发现仅负责获取 Broker 地址和端口。更新全局凭证后需要重启相关 Broker/后端容器使新配置完全生效。

## 6. 后续验收

- 长时间设备持续上报、断网恢复和多设备并发。
- 固定 GHCR 版本标签、镜像签名和漏洞扫描。
- TLS、每设备凭证及最小权限 ACL。

## 7. HiveMQ 远程版本（实机验收通过）

`Remote-Hivemq` 分支另提供远程编排 `docker-compose.remote.ghcr.yml`。源码中的远程 GHCR 编排已准备固定使用 `v1.1.7-remote-mqtt`，群晖当前仍运行 `v1.1.6-remote-mqtt`；升级时必须继续复用原 MySQL 数据卷和私有环境文件。编排只启动 `mysql`、`backend` 和 `frontend`：群晖后端主动通过 HiveMQ 域名的 TLS `8883` 订阅日志，**不**启动 Mosquitto，**不**暴露 `1883` 或 UDP `19830`。旧远程标签保留用于回退。局域网版可固定拉取 `v1.0.0-lan`，`latest` 也保持局域网语义并且只由 `main` 分支更新，两种模式不会混用。

使用 Windows 上的以下命令生成远程成品镜像部署包：

```powershell
tools/create-synology-image-deploy.ps1 -Remote -OutputDirectory dist/synology-remote-image-deploy
```

将生成目录上传到 NAS 后，先在该目录执行：

```sh
sudo -i
sh tools/initialize-synology-docker-config.sh
```

初始化器只会创建被忽略的 `docker/local/hivemq-remote.env`；在其中填写私有 `MQTT_BROKER_URL=ssl://<private-host>:8883`、`MQTT_USERNAME` 与 `MQTT_PASSWORD` 后，再以 `docker-compose.yml` 创建 Container Manager 项目。不得上传、提交或截图展示该文件。该模式不需要群晖公网 IP 或路由器端口转发，管理网页如需外网访问应另行通过受认证的 VPN/反向代理规划，而不是暴露 MQTT 或数据库端口。

2026-07-15 已完成远程项目实机验收：群晖 Docker CLI 不带 Compose V2 插件，故通过 DSM Container Manager 创建项目。NAS 既有服务占用宿主机 `8080` 时，后端首次启动报 external connectivity；将后端映射改为 `18080:8080`、前端改为 `18000:80` 后启动成功，容器内部服务地址未改变。MySQL 健康，后端连接 HiveMQ TLS，MQTTX 测试日志的“收到消息”和“处理成功”计数均增加。原局域网项目及数据保留但未与远程项目同时运行；记录中不包含真实 NAS 地址或 HiveMQ 凭证。

2026-07-17 已完成远程项目升级：群晖成功拉取 `aiot-log-backend:v1.1.4-remote-mqtt` 和 `aiot-log-frontend:v1.1.4-remote-mqtt`，并使用同一远程项目重新创建前后端服务；原 MySQL 数据卷和私有 `docker/local/hivemq-remote.env` 继续复用。该版本增加日志详情实时同步和本地 AI 回退事件中文化；后续浏览器验收应先强制刷新静态资源，再用真机确认详情内容实时追加。

2026-07-20 长时间运行观察发现 `v1.1.4-remote-mqtt` 的日志页可能在首次 API 请求失败后没有创建轮询；切换菜单重新挂载后恢复，设备到数据库链路本身正常。`v1.1.5-remote-mqtt` 调整为先建立轮询和恢复监听，再异步加载数据，并在页面可见、窗口聚焦、网络恢复和 `pageshow` 时自愈；同时在顶部显示后端进程运行时长。群晖已在原远程项目中升级前后端到该版本，复用当前 MySQL 卷和私有环境文件且未使用 `down -v`。浏览器强制刷新后确认运行时长逐秒增加、MQTT 已连接、小智重启日志无需切换菜单即可约 1 秒显示，标签页隐藏后返回也会立即补刷。

2026-07-20 已继续升级至 `v1.1.6-remote-mqtt`：先验证 Compose 配置并拉取固定标签的前后端镜像，再使用原项目名原地更新，没有执行 `down -v`。原 MySQL 数据卷、私有环境文件及宿主机端口映射保持不变；前端、后端和 MySQL 均正常运行，后端容器显示 `healthy`，确认只监听容器回环地址的 Actuator 健康检查和前端 `service_healthy` 就绪依赖已生效。

2026-07-21 已完成约 20 小时连续运行后的正常重启及手动停止/启动验收。API、远程 MQTT TLS、日志页轮询和后端运行时长均能自行恢复，原 40 条日志及 MySQL 数据保持不变。停止期间前端静态页仍可访问并显示请求失败，但顶部固定显示“后端已接入”，会误报后端在线；需要改为动态状态。MQTTX 失败、恢复、再次失败三条消息均处理成功，但当失败与恢复相隔超过 30 秒时，恢复会新建 `RESOLVED` 记录并遗留旧 `PENDING` 故障；后续需修复跨窗口故障关联并回归。Container Manager 的完整 `healthy` 状态变化尚未单独留证。

同日上述两项问题已在本地源码修复并通过 15 项后端测试及前端生产构建，但尚未生成新远程镜像或升级群晖。下一次部署必须继续复用原 MySQL 数据卷和私有环境文件，不得使用 `down -v`；升级后再复测顶部离线状态、跨窗口故障关闭和 Container Manager 的 `healthy` 状态变化。
