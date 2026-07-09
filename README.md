# AIoT 智能设备运行日志管理系统

基于 Vue 3、Spring Boot 3、MySQL 和 MQTT 的设备运行数据与日志管理系统。

## 已实现功能

- 设备、日志、标签和告警规则管理
- HTTP 与 MQTT 设备自动上报
- 上报数据、趋势和 MQTT 状态展示
- 异常上报自动生成设备日志
- 本地脚本启动与 Docker Compose 一键部署

## Docker 一键启动

环境要求：已安装并启动 Docker Desktop。

Windows 可双击：

```text
docker-start.cmd
```

首次部署或项目代码更新后，双击：

```text
docker-update.cmd
```

对应命令：

```bash
docker compose up -d
docker compose up -d --build
```

网页地址：<http://127.0.0.1/>

日常启动不会重新构建；停止服务可双击 `docker-stop.cmd`。详细说明见 [项目设计说明](docs/project-design.md)。

## 开发环境启动

使用已经配置好的本机 Java、Node.js、MySQL 和 Mosquitto 时，双击 `start-all.cmd`，停止时双击 `stop-all.cmd`。

## 真实设备接入

Docker 会部署服务，但不会自动连接 WiFi 或配置真实设备。设备需要能够访问部署电脑的局域网 IP。

- MQTT：`tcp://<电脑局域网IP>:1883`
- MQTT Topic：`aiot/device/{deviceCode}/report`
- HTTP：`http://<电脑局域网IP>:8080/api/device-reports`

MQTT 数据格式见 [项目设计说明](docs/project-design.md)。

## 项目文档

文档索引见 [docs/README.md](docs/README.md)，当前进度见 [docs/project-status.md](docs/project-status.md)。
