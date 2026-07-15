# 群晖 NAS 实际部署记录

更新时间：2026-07-14

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

## 7. HiveMQ 远程版本（待实机验收）

`Remote-Hivemq` 分支另提供远程编排 `docker-compose.remote.ghcr.yml`。该编排固定拉取 GHCR 的 `v1.1.0-remote-mqtt` 镜像标签，只启动 `mysql`、`backend` 和 `frontend`：群晖后端主动通过 HiveMQ 域名的 TLS `8883` 订阅日志，**不**启动 Mosquitto，**不**暴露 `1883` 或 UDP `19830`。局域网版可固定拉取 `v1.0.0-lan`，`latest` 也保持局域网语义并且只由 `main` 分支更新，两种镜像不会混用。

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
