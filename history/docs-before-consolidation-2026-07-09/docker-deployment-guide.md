# Docker 一键部署说明

更新时间：2026-07-09

## 1. 一键部署包含什么

Docker Compose 会自动构建并启动：

- Vue 前端和 Nginx
- Spring Boot 后端
- MySQL 8.4
- Mosquitto MQTT Broker

容器会自动连接，不需要分别安装 Java、Maven、Node.js、MySQL 和 Mosquitto。

Docker 不负责连接 WiFi，也不会自动修改真实设备的网络配置。运行系统的电脑必须先正常联网；真实设备需要与电脑网络互通，并填写电脑的局域网 IP。

## 2. 环境要求

Windows 用户安装并启动 Docker Desktop，然后确认：

```powershell
docker version
docker compose version
```

首次构建需要联网下载基础镜像和依赖。

## 3. 启动与停止

双击根目录：

```text
docker-start.cmd
```

脚本会先等待后端 API，再等待网页真正可访问，最后才打开浏览器，避免 Spring Boot 尚未就绪时页面出现 `502`。

脚本职责：

- `docker-start.cmd`：日常快速启动已有版本，不重新构建。
- `docker-update.cmd`：首次部署或代码更新后重新构建并启动。
- `docker-stop.cmd`：停止服务并保留数据库数据。

或执行：

```powershell
docker compose up -d
```

首次部署或更新项目：

```powershell
docker compose up -d --build
```

启动完成后访问：

```text
管理网页：http://127.0.0.1/
后端接口：http://127.0.0.1:8080/
MQTT：127.0.0.1:1883
MySQL：127.0.0.1:3306
```

双击 `docker-stop.cmd` 或执行以下命令停止：

```powershell
docker compose down
```

停止不会删除数据库数据。需要删除全部容器数据时才使用：

```powershell
docker compose down -v
```

## 4. 真实设备如何连接

在部署电脑执行 `ipconfig`，找到当前网卡的 IPv4 地址，例如 `192.168.1.20`。

MQTT 设备配置：

```text
Broker：192.168.1.20
Port：1883
Topic：aiot/device/{deviceCode}/report
```

HTTP 设备上报地址：

```text
http://192.168.1.20:8080/api/device-reports
```

设备与电脑必须处于可互通的网络，并且 Windows 防火墙需要允许 TCP `1883` 和 `8080` 入站。

## 5. 配置端口

复制 `.env.example` 为 `.env` 后可以修改默认密码和端口。修改 `WEB_PORT` 后，网页地址也要使用对应端口。

如果本机已有 MySQL、Mosquitto 或后端占用了 `3306`、`1883`、`8080`，应先停止本地服务，或者在 `.env` 中修改宿主机端口。

## 6. 查看状态与日志

```powershell
docker compose ps
docker compose logs -f backend
docker compose logs -f mosquitto
```

数据库初始化脚本只会在首次创建数据库卷时执行。修改 SQL 后若要完全重新初始化，必须先备份数据，再删除数据库卷。

## 7. 当前安全限制

当前 Mosquitto 为方便局域网联调启用了匿名访问，只适合本机和可信局域网演示，不可直接暴露到公网。正式部署前需要增加 MQTT 用户名、密码或 TLS，并限制防火墙访问来源。
